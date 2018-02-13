node('maven') {
   	// define commands
   	def mvnCmd = "mvn -s configuration/maven-cicd-settings.xml"
   	def DEV_PROJECT = "reportengine-dev"
   	def QA_PROJECT = "reportengine-qa"
   	def PROD_PROJECT = "reportengine-prod"
   	def PORT = 8080
   	def APP_NAME = "identity-server"
 
   	stage ('Build') {
   		git branch: 'master', url: 'https://github.com/vargadan/identity-server.git'
   		sh "${mvnCmd} clean package -DskipTests=true"
   	}
   	
   	def version = version()

   	stage ('Deploy DEV') {
   		sh "oc project ${DEV_PROJECT}"
	   // create build. override the exit code since it complains about exising imagestream
	   sh "${mvnCmd} fabric8:deploy -DskipTests"
	   //tag for version in DEV imagestream
	   sh "oc tag ${DEV_PROJECT}/${APP_NAME}:latest ${DEV_PROJECT}/${APP_NAME}:${version}"
	}

   stage ('Promote to QA') {
     	timeout(time:10, unit:'MINUTES') {
        		input message: "Promote to IT?", ok: "Promote"
        }
        //put into QA imagestream
        sh "oc tag ${DEV_PROJECT}/${APP_NAME}:latest ${QA_PROJECT}/${APP_NAME}:latest"
	}
}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}
