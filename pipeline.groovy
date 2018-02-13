node('maven') {
   	// define commands
   	def mvnCmd = "mvn -s configuration/maven-cicd-settings.xml"
   	def CICD_PROJECT = "reportengine-cicd"
   	def DEV_PROJECT = "reportengine-dev"
   	def QA_PROJECT = "reportengine-qa"
   	def PROD_PROJECT = "reportengine-prod"
   	def PORT = 8080
   	def APP_NAME = "identity-server"
 
   	stage ('Build') {
   		git branch: 'master', url: 'https://github.com/vargadan/identity-server.git'
   		sh "${mvnCmd} clean package -DskipTests=true fabric8:build"
   	}
   	
   	def version = version()

   	stage ('Deploy DEV') {
	   // create build. override the exit code since it complains about exising imagestream
	   //tag for version in DEV imagestream
	   sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${CICD_PROJECT}/${APP_NAME}:promotedToDEV"
	}

   stage ('Promote to QA') {
     	timeout(time:10, unit:'MINUTES') {
        		input message: "Promote to IT?", ok: "Promote"
        }
        //put into QA imagestream
        sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${CICD_PROJECT}/${APP_NAME}:promotedToQA"
	}
	
   stage ('Promote to PROD') {
     	timeout(time:10, unit:'MINUTES') {
        		input message: "Promote to IT?", ok: "Promote"
        }
        //put into QA imagestream
        sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${CICD_PROJECT}/${APP_NAME}:promotedToPROD"
	}
}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}
