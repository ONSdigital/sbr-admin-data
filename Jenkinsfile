#!/usr/bin/env groovy

def artServer = Artifactory.server 'art-p-01'
def buildInfo = Artifactory.newBuildInfo()
def distDir = 'build/dist/'
def agentSbtVersion = 'sbt_0-13-13'

pipeline {
    libraries {
        lib('jenkins-pipeline-shared@feature/cfdeploy-with-env-vars')
    }
    environment {
        SVC_NAME = "sbr-admin-data"

        GROUP = "ons"
        CF_ORG = "sbr"
        CF_API_ENDPOINT = credentials('cfApiEndpoint')

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

        // stage('Build') {
        //     agent { label "build.${agentSbtVersion}" }
        //     steps{
        //         unstash name: 'Checkout'
        //         sh "sbt compile"
        //     }
        //     post {
        //         success {
        //             postSuccess()
        //         }
        //         failure {
        //             postFail()
        //         }
        //     }
        // }

        // stage('Validate'){
        //     failFast true
        //     parallel {
        //         stage('Test: Unit') {
        //             agent { label "build.${agentSbtVersion}" }
        //             steps {
        //                 unstash name: 'Checkout'
        //                 sh 'sbt coverage test coverageReport coverageAggregate'
        //             }
        //             post {
        //                 always {
        //                     junit '**/target/test-reports/*.xml'
        //                     cobertura coberturaReportFile: 'target/**/coverage-report/*.xml'
        //                 }
        //                 success {
        //                     postSuccess()
        //                 }
        //                 failure {
        //                     postFail()
        //                 }
        //             }
        //         }
        //         stage('Style') {
        //             agent { label "build.${agentSbtVersion}" }
        //             environment{ STAGE = "Static Analysis" }
        //             steps {
        //                 unstash name: 'Checkout'
        //                 sh "sbt scalastyle"
        //             }
        //             post {
        //                 always {
        //                     checkstyle pattern: '**/target/code-quality/style/*scalastyle*.xml'
        //                 }
        //                 success {
        //                     postSuccess()
        //                 }
        //                 failure {
        //                     postFail()
        //                 }
        //             }
        //         }
        //     }
        // }

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
                unstash name: 'Checkout'
                dir('config') {
                    git(url: "${GITLAB_URL}/StatBusReg/${SVC_NAME}-api.git", credentialsId: 'JenkinsSBR__gitlab', branch: "develop")
                }
                // Replace fake VAT/PAYE data with real data
                sh '''
                rm -rf conf/sample/201706/vat_data.csv
                rm -rf conf/sample/201706/paye_data.csv
                cp config/dev/data/sbr-2500-ent-vat-data.csv conf/sample/201706/vat_data.csv
                cp config/dev/data/sbr-2500-ent-paye-data.csv conf/sample/201706/paye_data.csv
                cp config/dev/conf/* conf
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
                stash name: 'Config', includes: 'config/'
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

        stage('Deploy: Dev CF'){
            agent { label 'deploy.cf'}
            when{ 
                branch 'feature/REG-428-continuous-delivery'
                beforeAgent true
            }
            environment{
                CREDS = 's_jenkins_sbr_dev'
                DEPLOY_TO = 'dev' 
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
                sh "cp ${distDir}/${env.GROUP}-${env.SVC_NAME}-*.zip ${env.DEPLOY_TO}-${env.GROUP}-${env.SVC_NAME}.zip"
                unstash name: 'Config'
                milestone(1)
                lock("${env.DEPLOY_TO}-${env.CH_TABLE}-${env.SVC_NAME}") {
                    deployToCloudFoundry("${CREDS}", "${env.CH_TABLE}")
                }
                lock("${env.DEPLOY_TO}-${env.VAT_TABLE}-${env.SVC_NAME}") {
                    deployToCloudFoundry("${envCREDS}", "${env.VAT_TABLE}")
                }
                lock("${env.DEPLOY_TO}-${env.PAYE_TABLE}-${env.SVC_NAME}") {
                    deployToCloudFoundry("${env.CREDS}", "${env.PAYE_TABLE}")
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

        // stage ('Deploy: Dev Hbase') {
        //     agent any
        //     when{ 
        //         branch 'feature/REG-428-continuous-delivery'
        //         beforeAgent true
        //     }
        //     environment {
        //         DEPLOY_TO = 'dev'
        //     }
        //     steps {
        //         unstage name: 'Package'
        //         sh "sbt 'set test in assembly := {}' assembly"
        //         copyToHBaseNode()
        //     }
        //     post {
        //         success {
        //             postSuccess()
        //         }
        //         failure {
        //             postFail()
        //         }
        //     }
        // }
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

// deployToCloudFoundry calls pushToCloudFoundry with environment variables set
def deployToCloudFoundry (String credentialsId, String tablename) {
    colourText("info", "${env.DEPLOY_TO}-${tablename}-${env.SVC_NAME} deployment in progress")
    withCredentials([usernamePassword(credentialsId: credentialsId, passwordVariable: 'KB_PASSWORD', usernameVariable: 'KB_USERNAME')]){
        cfDeploy{
            credentialsId = credentialsId
            org = "${this.env.CF_ORG}"
            space = "${this.env.DEPLOY_TO}"
            appName = "${tablename}-${this.env.SVC_NAME}",
            appPath = "./${this.env.DEPLOY_TO}-${this.env.GROUP}-${this.env.SVC_NAME}.zip"
            manifestPath = "config/${this.env.DEPLOY_TO.toLowerCase()}/manifest.yml",
            envVars = [
                'HBASE_AUTHENTICATION_USERNAME': "${this.env.KB_USERNAME}",
                'HBASE_AUTHENTICATION_PASSWORD': "${this.env.KB_PASSWORD}",
                'HBASE_NAMESPACE': "sbr_${this.env.DEPLOY_TO}_db",
                'HBASE_TABLE_NAME': "${tablename}",
                'HBASE_LOAD_REVERSE_FLAG': "false"
            ]
        }
    }
    colourText("success", "${env.DEPLOY_TO}-${tablename}-${env.SVC_NAME} Deployed.")
}

def copyToHBaseNode() {
    echo "Deploying to ${env.DEPLOY_TO}"
    sshagent(credentials: ["sbr-${env.DEPLOY_TO}-ci-ssh-key"]) {
        withCredentials([string(credentialsId: "sbr-hbase-node", variable: 'HBASE_NODE')]) {
            sh """
                ssh sbr-${env.DEPLOY_TO}-ci@${env.HBASE_NODE} mkdir -p ${env.SVC_NAME}/lib
                scp ${WORKSPACE}/target/ons-sbr-admin-data-*.jar sbr-${env.DEPLOY_TO}-ci@${env.HBASE_NODE}:${env.SVC_NAME}/lib/
                echo 'Successfully copied jar file to ${env.SVC_NAME}/lib directory on ${env.HBASE_NODE}'
                ssh sbr-${env.DEPLOY_TO}-ci@${env.HBASE_NODE} hdfs dfs -put -f ${env.SVC_NAME}/lib/ons-sbr-admin-data-*.jar hdfs://prod1/user/sbr-${env.DEPLOY_TO}-ci/lib/
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