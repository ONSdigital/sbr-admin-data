
pipeline {
    agent any
    environment {
       RELEASE_TYPE = "PATCH"

        BRANCH_DEV = "develop"
        BRANCH_TEST = "release"
        BRANCH_PROD = "master"

        DEPLOY_DEV = "dev"
        DEPLOY_TEST = "test"
        DEPLOY_PROD = "prod"

        CF_CREDS = "sbr-api-dev-secret-key"

        GIT_TYPE = "Github"
        GIT_CREDS = "github-sbr-user"
        GITLAB_CREDS = "sbr-gitlab-id"

        ORGANIZATION = "ons"
        TEAM = "sbr"
        MODULE_NAME = "sbr-admin-data"

        // hbase config
        TABLE_NAME = "enterprise"
        NAMESPACE = "sbr_dev_db"
    }
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    stages {
	stage('Checkout') {
            agent any
            steps {
                deleteDir()
                checkout scm
                stash name: 'app'
                sh "$SBT version"
                script {
                    version = '1.0.' + env.BUILD_NUMBER
                    currentBuild.displayName = version
                    env.NODE_STAGE = "Checkout"
                }
            }
        }
        stage('Build'){
	    agent any
            steps {
		 // $SBT clean compile coverage test coverageReport coverageAggregate "project $MODULE_NAME" universal:packageBin
                  sh """
		    echo "current workspace 1 ${WORKSPACE}"
		    $SBT clean compile assembly
               
                  """
		  copyToHBaseNode()
		    // scp ${WORKSPACE}/target/ons-sbr-admin-data-assembly-0.1.0-SNAPSHOT.jar sbr-dev-ci@cdhdn-p01-01:dev/sbr-hbase-connector/lib
			//  echo "Successfully copied jar file to sbr-hbase-connector/lib directory on cdhdn-p01-01"
            }
          
        }
        
        stage('Deploy') {
            steps {
		      //bundleApp()
		   sh "echo deploy workspace ${WORKSPACE}"
		  //copyToHBaseNode()  	 
            }
        }
    }
}

def push (String newTag, String currentTag) {
    echo "Pushing tag ${newTag} to Gitlab"
    GitRelease( GIT_CREDS, newTag, currentTag, "${env.BUILD_ID}", "${env.BRANCH_NAME}", GIT_TYPE)
}

def deploy () {
    echo "Deploying Api app to ${env.DEPLOY_NAME}"
    withCredentials([string(credentialsId: CF_CREDS, variable: 'APPLICATION_SECRET')]) {
        deployToCloudFoundryHBase("cloud-foundry-$TEAM-${env.DEPLOY_NAME}-user", TEAM, "${env.DEPLOY_NAME}", "${env.DEPLOY_NAME}-$MODULE_NAME", "${env.DEPLOY_NAME}-${ORGANIZATION}-${MODULE_NAME}.zip", "gitlab/${env.DEPLOY_NAME}/manifest.yml", TABLE_NAME, NAMESPACE)
    }
}

def copyToHBaseNode() {
    echo "Deploying to $DEPLOY_DEV"
    sshagent(credentials: ["sbr-$DEPLOY_DEV-ci-ssh-key"]) {
        withCredentials([string(credentialsId: "sbr-hbase-node", variable: 'HBASE_NODE')]) {
            sh '''
                scp ${WORKSPACE}/target/ons-sbr-admin-data-*.jar sbr-$DEPLOY_DEV-ci@$HBASE_NODE:$DEPLOY_DEV/$MODULE_NAME/lib
		        echo "Successfully copied jar file to $MODULE_NAME/lib directory on $HBASE_NODE"
	       ssh sbr-$DEPLOY_DEV-ci@$HBASE_NODE hdfs dfs -put -f $DEPLOY_DEV/$MODULE_NAME/lib/ons-sbr-admin-data-*.jar hdfs://prod1/user/sbr-$DEPLOY_DEV-ci/lib/
		echo "Successfully copied jar file to HDFS"
	    '''
        }
    }
}
