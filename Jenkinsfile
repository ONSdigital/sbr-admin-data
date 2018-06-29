#!groovy
@Library('jenkins-pipeline-shared') _

pipeline {
    environment {
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
        LEU_TABLE = "leu"
	    
    	STAGE = "NONE"
        SBT_HOME = tool name: 'sbt.13.13', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'
        PATH = "${env.SBT_HOME}/bin:${env.PATH}"
    }
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    agent any
    stages {
        stage('Checkout') {
            agent any
            environment{ STAGE = "Checkout" }
            steps {
                deleteDir()
                checkout scm
                stash name: 'app'
                sh "sbt version"
                script {
                    version = '1.0.' + env.BUILD_NUMBER
                    currentBuild.displayName = version
                }
            }
        }

        stage('Build') {
            agent any
            environment{ STAGE = "Build" }
            steps{
                sh "sbt clean compile"
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
                }
            }
        }

        stage('Test'){
            agent any
            environment{ STAGE = "Test"  }
            steps {
                colourText("info", "Building ${env.BUILD_ID} on ${env.JENKINS_URL} from branch ${env.BRANCH_NAME}")

                sh 'sbt coverage test coverageReport coverageAggregate'
            }
            post {
                success {
                    junit '**/target/test-reports/*.xml'
                    step([$class: 'CoberturaPublisher', coberturaReportFile: '**/target/coverage-report/*.xml'])
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
                }
            }
        }

        stage('Static Analysis') {
            agent any
            environment{ STAGE = "Static Analysis" }
            steps {
                parallel (
                    "Scalastyle" : {
                        colourText("info","Running scalastyle analysis")
                        sh "sbt scalastyle"
                    },
                    "Scapegoat" : {
                        colourText("info","Running scapegoat analysis")
                        sh "sbt scapegoat"
                    }
                )
            }
            post {
                success {
                    step([$class: 'CheckStylePublisher', pattern: '**/target/code-quality/style/*scalastyle*.xml'])
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
                }
            }
        }

        stage('Package'){
            agent any
            when{ expression{ isBranch("master") }}
            environment{ 
                STAGE = "Package" 
                DEPLOY_TO = "dev"    
            }
            steps {
                dir('gitlab') {
                    git(url: "$GITLAB_URL/StatBusReg/${MODULE_NAME}-api.git", credentialsId: GITLAB_CREDS, branch: "develop")
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
                sh "cp target/universal/${ORGANIZATION}-${MODULE_NAME}-*.zip ${DEPLOY_TO}-${ORGANIZATION}-${MODULE_NAME}.zip"
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
                }
            }
        }

        stage('Deploy CF'){
            agent any
            when{ expression{ isBranch("master") }}
            environment{ 
                STAGE = "Deploy CF"
                DEPLOY_TO = "dev" 
            }
            steps {
                milestone(1)
                lock('CH Deployment Initiated') {
                    colourText("info", "${env.DEPLOY_TO}-${CH_TABLE}-${MODULE_NAME} deployment in progress")
                    deploy(CH_TABLE, false)
                    colourText("success", "${env.DEPLOY_TO}-${CH_TABLE}-${MODULE_NAME} Deployed.")
                }
                lock('VAT Deployment Initiated') {
                    colourText("info", "${env.DEPLOY_TO}-${VAT_TABLE}-${MODULE_NAME} deployment in progress")
                    deploy(VAT_TABLE, false)
                    colourText("success", "${env.DEPLOY_TO}-${VAT_TABLE}-${MODULE_NAME} Deployed.")
                }
                lock('PAYE Deployment Initiated') {
                    colourText("info", "${env.DEPLOY_TO}-${PAYE_TABLE}-${MODULE_NAME} deployment in progress")
                    deploy(PAYE_TABLE, false)
                    colourText("success", "${env.DEPLOY_TO}-${PAYE_TABLE}-${MODULE_NAME} Deployed.")
                }
                lock('Legal Unit Deployment Initiated') {
                    colourText("info", "${env.DEPLOY_TO}-${LEU_TABLE}-${MODULE_NAME} deployment in progress")
                    deploy(LEU_TABLE , true)
                    colourText("success", "${env.DEPLOY_TO}-${LEU_TABLE}-${MODULE_NAME} Deployed.")
                }
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
                }
            }
        }

        stage ('Deploy HBase') {
            agent any
            when{ expression{ isBranch("master") }}
            environment {
                STAGE = "Deploy HBase"
                DEPLOY_TO = "dev"
            }
            steps {
                sh "sbt 'set test in assembly := {}' assembly"
                copyToHBaseNode()
            }
            post {
                success {
                    colourText("info","Stage: ${env.STAGE} successful!")
                }
                failure {
                    colourText("warn","Stage: ${env.STAGE} failed!")
                }
            }
        }
    }
    post {
        success {
            colourText("success", "All stages complete. Build was successful.")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST"
        }
        unstable {
            colourText("warn", "Something went wrong, build finished with result ${currentResult}. This may be caused by failed tests, code violation or in some cases unexpected interrupt.")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST", "${env.STAGE}"
        }
        failure {
            colourText("warn","Process failed at: ${env.STAGE}")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST", "${env.STAGE}"
        }
    }
}

def isBranch(String branchName){
    return env.BRANCH_NAME.toString().equals(branchName)
}

def deploy (String DATA_SOURCE, Boolean REVERSE_FLAG) {
    CF_SPACE = "${env.DEPLOY_TO}".capitalize()
    CF_ORG = "${TEAM}".toUpperCase()
    NAMESPACE = "sbr_${env.DEPLOY_TO}_db"
    echo "Deploying Api app to ${env.DEPLOY_TO}"
    withCredentials([string(credentialsId: CF_CREDS, variable: 'APPLICATION_SECRET')]) {
        deployToCloudFoundryHBaseWithReverseOption("${TEAM}-${env.DEPLOY_TO}-cf", "${CF_ORG}", "${CF_SPACE}", "${env.DEPLOY_TO}-${DATA_SOURCE}-${MODULE_NAME}", "${env.DEPLOY_TO}-${ORGANIZATION}-${MODULE_NAME}.zip", "gitlab/${env.DEPLOY_TO}/manifest.yml", "${DATA_SOURCE}", "${NAMESPACE}", REVERSE_FLAG)
    }
}

def copyToHBaseNode() {
    echo "Deploying to ${env.DEPLOY_TO}"
    sshagent(credentials: ["sbr-${env.DEPLOY_TO}-ci-ssh-key"]) {
        withCredentials([string(credentialsId: "sbr-hbase-node", variable: 'HBASE_NODE')]) {
            sh """
                ssh sbr-${env.DEPLOY_TO}-ci@${HBASE_NODE} mkdir -p ${MODULE_NAME}/lib
                scp ${WORKSPACE}/target/ons-sbr-admin-data-*.jar sbr-${env.DEPLOY_TO}-ci@${HBASE_NODE}:${MODULE_NAME}/lib/
                echo 'Successfully copied jar file to ${MODULE_NAME}/lib directory on ${HBASE_NODE}'
                ssh sbr-${env.DEPLOY_TO}-ci@${HBASE_NODE} hdfs dfs -put -f ${MODULE_NAME}/lib/ons-sbr-admin-data-*.jar hdfs://prod1/user/sbr-${env.DEPLOY_TO}-ci/lib/
                echo 'Successfully copied jar file to HDFS'
	    """
        }
    }
}
