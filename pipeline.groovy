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
	   	sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${CICD_PROJECT}/${APP_NAME}:version"
	   	sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${DEV_PROJECT}/${APP_NAME}:latest"
		envSetup(DEV_PROJECT, APP_NAME, 'latest', true)
	}

   	stage ('Promote to QA') {
     	timeout(time:10, unit:'MINUTES') {
        		input message: "Promote to QA?", ok: "Promote"
        }
        //put into QA imagestream
        sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${QA_PROJECT}/${APP_NAME}:latest"
        envSetup(QA_PROJECT, APP_NAME, 'latest', false)
	}

}

def envSetup(project, appName, version, recreate) {
	if (recreate) {
		sh "oc delete buildconfig,deploymentconfig,service,routes -l app=${appName} -n ${project}"
	}
   	sh "oc new-app ${appName}:${version} -n ${project}"
   	sh "oc expose svc ${appName} -n ${project}"
}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}
