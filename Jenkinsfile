#!/usr/bin/env groovy

def artServer = Artifactory.server 'art-p-01'
def buildInfo = Artifactory.newBuildInfo()
def distDir = 'build/dist'
def agentSbtVersion = 'sbt_0-13-13'

pipeline {
    libraries {
        lib('jenkins-pipeline-shared')
    }
    environment {
        SVC_NAME = "sbr-admin-data"
        RELEASE_TYPE = "PATCH"
        CF_CREDS = "sbr-api-dev-secret-key"

        GIT_TYPE = "Github"
        GIT_CREDS = "github-sbr-user"
        GITLAB_CREDS = "sbr-gitlab-id"

        ORGANIZATION = "ons"
        TEAM = "sbr"
        MODULE_NAME = "sbr-admin-data"

        // hbase config
        CH_TABLE = "ch"
        VAT_TABLE = "vat"
        PAYE_TABLE = "paye"
    }
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        ansiColor('xterm')
    }
    agent { label 'download.jenkins.slave'}
    stages {
        stage('Checkout') {
            agent { label 'download.jenkins.slave'}
            steps {
                checkout scm
                script {
                    buildInfo.name = "${SVC_NAME}"
                    buildInfo.number = "${BUILD_NUMBER}"
                    buildInfo.env.collect()
                }
                colourText("info", "BuildInfo: ${buildInfo.name}-${buildInfo.number}")
                stash name: 'Checkout'
            }
        }

        stage('Build') {
            agent { label "build.${agentSbtVersion}" }
            steps{
                unstash name: 'Checkout'
                sh "sbt compile"
            }
            post {
                success {
                    postSuccess()
                }
                failure {
                    postFail()
                }
            }
        }

        stage('Validate'){
            failFast true
            parallel {
                stage('Test: Unit') {
                    agent { label "build.${agentSbtVersion}" }
                    steps {
                        unstash name: 'Checkout'
                        sh 'sbt coverage test coverageReport coverageAggregate'
                    }
                    post {
                        always {
                            junit '**/target/test-reports/*.xml'
                            cobertura coberturaReportFile: 'target/**/coverage-report/*.xml'
                        }
                        success {
                            postSuccess()
                        }
                        failure {
                            postFail()
                        }
                    }
                }
                stage('Style') {
                    agent { label "build.${agentSbtVersion}" }
                    environment{ STAGE = "Static Analysis" }
                    steps {
                        unstash name: 'Checkout'
                        sh "sbt scalastyle"
                    }
                    post {
                        always {
                            checkstyle pattern: '**/target/code-quality/style/*scalastyle*.xml'
                        }
                        success {
                            postSuccess()
                        }
                        failure {
                            postFail()
                        }
                    }
                }
            }
        }

        stage('Package'){
            agent { label "build.${agentSbtVersion}" }
            when{ 
                branch 'feature/REG-428-continuous-delivery'
                beforeAgent true
            }
            environment{ 
                STAGE = "Package" 
                DEPLOY_TO = "dev"    
            }
            steps {
                dir('gitlab') {
                    git(url: "$GITLAB_URL/StatBusReg/${SVC_NAME}-api.git", credentialsId: 'JenkinsSBR__gitlab', branch: "develop")
                }
                // Replace fake VAT/PAYE data with real data
                sh '''
                rm -rf conf/sample/201706/vat_data.csv
                rm -rf conf/sample/201706/paye_data.csv
                cp gitlab/dev/data/sbr-2500-ent-vat-data.csv conf/sample/201706/vat_data.csv
                cp gitlab/dev/data/sbr-2500-ent-paye-data.csv conf/sample/201706/paye_data.csv
                cp gitlab/dev/conf/* conf
                '''

                sh 'sbt universal:packageBin'
                script {
                    def uploadSpec = """{
                        "files": [
                            {
                                "pattern": "target/universal/*.zip",
                                "target": "registers-sbt-snapshots/uk/gov/ons/${buildInfo.name}/${buildInfo.number}/"
                            }
                        ]                            
                    }"""
                    artServer.upload spec: uploadSpec, buildInfo: buildInfo
                }
                sh "cp target/universal/${ORGANIZATION}-${MODULE_NAME}-*.zip ${DEPLOY_TO}-${ORGANIZATION}-${MODULE_NAME}.zip"
                stash name: 'Package'
            }
            post {
                success {
                    postSuccess()
                }
                failure {
                    postFail()
                }
            }
        }

        stage('Deploy CF'){
            agent { label 'deploy.cf'}
            when{ 
                branch 'feature/REG-428-continuous-delivery'
                beforeAgent true
            }
            environment{ 
                CREDS = 's_jenkins_sbr_dev'
                DEPLOY_TO = "dev" 
            }
            steps {
                script {
                    def downloadSpec = """{
                        "files": [
                            {
                                "pattern": "registers-sbt-snapshots/uk/gov/ons/${buildInfo.name}/${buildInfo.number}/*.zip",
                                "target": "${distDir}",
                                "flat": "true"
                            }
                        ]
                    }"""
                    artServer.download spec: downloadSpec, buildInfo: buildInfo
                }
                unstash name: 'Package'
                milestone(1)
                lock("${env.DEPLOY_TO}-${CH_TABLE}-${MODULE_NAME}") {
                    colourText("info", "${env.DEPLOY_TO}-${CH_TABLE}-${MODULE_NAME} deployment in progress")
                    deploy(CH_TABLE, false)
                    colourText("success", "${env.DEPLOY_TO}-${CH_TABLE}-${MODULE_NAME} Deployed.")
                }
                lock("${env.DEPLOY_TO}-${VAT_TABLE}-${MODULE_NAME}") {
                    colourText("info", "${env.DEPLOY_TO}-${VAT_TABLE}-${MODULE_NAME} deployment in progress")
                    deploy(VAT_TABLE, false)
                    colourText("success", "${env.DEPLOY_TO}-${VAT_TABLE}-${MODULE_NAME} Deployed.")
                }
                lock("${env.DEPLOY_TO}-${PAYE_TABLE}-${MODULE_NAME}") {
                    colourText("info", "${env.DEPLOY_TO}-${PAYE_TABLE}-${MODULE_NAME} deployment in progress")
                    deploy(PAYE_TABLE, false)
                    colourText("success", "${env.DEPLOY_TO}-${PAYE_TABLE}-${MODULE_NAME} Deployed.")
                }
            }
            post {
                success {
                    postSuccess()
                }
                failure {
                    postFail()
                }
            }
        }

        stage ('Deploy: Dev') {
            agent any
            when{ 
                branch 'feature/REG-428-continuous-delivery'
                beforeAgent true
            }
            environment {
                STAGE = "Deploy HBase"
                DEPLOY_TO = "dev"
            }
            steps {
                unstage name: 'Package'
                sh "sbt 'set test in assembly := {}' assembly"
                copyToHBaseNode()
            }
            post {
                success {
                    postSuccess()
                }
                failure {
                    postFail()
                }
            }
        }
    }
    post {
        success {
            colourText("success", "All stages complete. Build was successful.")
            slackSend(
                color: "good",
                message: "${env.JOB_NAME} success: ${env.RUN_DISPLAY_URL}"
            )
        }
        unstable {
            colourText("warn", "Something went wrong, build finished with result ${currentResult}. This may be caused by failed tests, code violation or in some cases unexpected interrupt.")
            slackSend(
                color: "warning",
                message: "${env.JOB_NAME} unstable: ${env.RUN_DISPLAY_URL}"
            )
        }
        failure {
            colourText("warn","Process failed at: ${env.STAGE}")
            slackSend(
                color: "danger",
                message: "${env.JOB_NAME} failed at ${env.STAGE_NAME}: ${env.RUN_DISPLAY_URL}"
            )
        }
    }
}

def deploy (String dataSource, Boolean reverseFlag) {
    cfSpace = "${env.DEPLOY_TO}".capitalize()
    cfOrg = "${env.TEAM}".toUpperCase()
    namespace = "sbr_${env.DEPLOY_TO}_db"
    echo "Deploying Api app to ${env.DEPLOY_TO}"
    withCredentials([string(credentialsId: "${env.CF_CREDS}", variable: 'APPLICATION_SECRET')]) {
        deployToCloudFoundryHBaseWithReverseOption("${env.TEAM}-${env.DEPLOY_TO}-cf",  // creds
            cfOrg,
            cfSpace, 
            "${env.DEPLOY_TO}-${dataSource}-${env.MODULE_NAME}",       // app name
            "${env.DEPLOY_TO}-${env.ORGANIZATION}-${env.MODULE_NAME}.zip",  // path to artifact
            "gitlab/${env.DEPLOY_TO}/manifest.yml",                 // path to manifest
            dataSource,                                       // hbase table name
            namespace,                                         // hbase namespace
            reverseFlag)                                           // hbase reverse load flag
    }
}

def copyToHBaseNode() {
    echo "Deploying to ${env.DEPLOY_TO}"
    sshagent(credentials: ["sbr-${env.DEPLOY_TO}-ci-ssh-key"]) {
        withCredentials([string(credentialsId: "sbr-hbase-node", variable: 'HBASE_NODE')]) {
            sh """
                ssh sbr-${env.DEPLOY_TO}-ci@${env.HBASE_NODE} mkdir -p ${env.MODULE_NAME}/lib
                scp ${WORKSPACE}/target/ons-sbr-admin-data-*.jar sbr-${env.DEPLOY_TO}-ci@${env.HBASE_NODE}:${env.MODULE_NAME}/lib/
                echo 'Successfully copied jar file to ${env.MODULE_NAME}/lib directory on ${env.HBASE_NODE}'
                ssh sbr-${env.DEPLOY_TO}-ci@${env.HBASE_NODE} hdfs dfs -put -f ${env.MODULE_NAME}/lib/ons-sbr-admin-data-*.jar hdfs://prod1/user/sbr-${env.DEPLOY_TO}-ci/lib/
                echo 'Successfully copied jar file to HDFS'
	    """
        }
    }
}

def postSuccess() {
    colourText('info', "Stage: ${env.STAGE_NAME} successfull!")
}

def postFail() {
    colourText('warn', "Stage: ${env.STAGE_NAME} failed!")
}