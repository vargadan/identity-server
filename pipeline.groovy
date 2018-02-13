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
   
}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}
