node('maven') {
	def APP_NAME = "identity-server"
   	// define commands
   	def mvnCmd = "mvn -s configuration/maven-cicd-settings.xml"
   	def CICD_PROJECT = "ctr-cicd"
   	def DEV_PROJECT = "ctr-dev"
   	def QA_PROJECT = "ctr-it"
   	def PROD_PROJECT = "ctr-prod"
   	def PORT = 8080
   	def GIT_URL = "https://github.com/vargadan/${APP_NAME}.git"
   	def SKIP_TEST = "true"
 
   	stage ('Build & Test') {
   		git branch: 'master', url: "${GIT_URL}"
   		sh "${mvnCmd} clean package -DskipTests=${SKIP_TEST} fabric8:build"
   	}
   	
   	def version = version()
   	
	stage ('Deploy DEV') {
	   	// create build. override the exit code since it complains about exising imagestream
	   	//tag for version in DEV imagestream
	   	sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${CICD_PROJECT}/${APP_NAME}:${version}"
	   	sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${DEV_PROJECT}/${APP_NAME}:latest"
		envSetup(DEV_PROJECT, APP_NAME, 'latest', true)
	}

   	stage ('Deploy to IT') {
        //put into IT imagestream
        sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${IT_PROJECT}/${APP_NAME}:latest"
        envSetup(IT_PROJECT, APP_NAME, 'latest', true)
	}
	
   	stage ('Deploy to PROD') {
        //put into PROD imagestream
        sh "oc tag ${CICD_PROJECT}/${APP_NAME}:latest ${PROD_PROJECT}/${APP_NAME}:latest"
        envSetup(PROD_PROJECT, APP_NAME, 'latest', false)
	}

}

def envSetup(project, appName, version, recreate) {
	GET_DC_OUT = sh (
		script: "oc get deploymentconfig -l app=${appName} -n ${project}",
		returnStdout: true
	).trim()
	echo "GET_DC_OUT : ${GET_DC_OUT}"
	appExists = GET_DC_OUT.contains(appName)
	if (appExists && recreate) {
		sh "oc delete deploymentconfig,service,routes -l app=${appName} -n ${project}"
		sh "oc new-app ${appName}:${version} -n ${project}"
   		sh "oc expose svc ${appName} -n ${project}"
   	} else if (!appExists) {
 		sh "oc new-app ${appName}:${version} -n ${project}"
   		sh "oc expose svc ${appName} -n ${project}"
   	}
}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}