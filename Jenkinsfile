
pipeline {
    agent any
    environment {
       DEPLOY_DEV = "dev"
       HBASE_CONNECTOR_DIR = "$DEPLOY_DEV/sbr-hbase-connector"
    }
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    stages {
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
                scp ${WORKSPACE}/target/ons-sbr-admin-data-assembly-0.1.0-SNAPSHOT.jar sbr-$DEPLOY_DEV-ci@$HBASE_NODE:$DEPLOY_DEV/sbr-hbase-connector/lib
		echo "Successfully copied jar file to sbr-hbase-connector/lib directory on $HBASE_NODE"
	    '''
        }
    }
}
