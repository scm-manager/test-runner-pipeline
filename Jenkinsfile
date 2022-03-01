#!groovy
pipeline {

  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds()
  }

  parameters {
    string(defaultValue: '', description: 'List of to be installed plugins (comma-separated)', name: 'Plugins', trim: true)
    choice(name: 'Log_Level', choices: ['info', 'debug'], description: 'Sets the log level for the test-runner execution')
  }

  agent none

  triggers {
    cron('H H(3-5) * * 1-5')
  }

  stages {
    stage('Get version') {
      agent {
        docker {
          image 'scmmanager/node-build:14.16.0'
          label 'docker'
        }
      }
      environment {
        HOME = "${env.WORKSPACE}"
      }
      steps {
        sh "yarn install"
        script {
          def tagVersion = sh(script: "node scripts/fetch-image-version.js", returnStdout: true).trim()
          imageTag = "cloudogu/scm-manager:" + tagVersion
        }
      }
    }
    stage('Trigger ci-plugin-snapshot build') {
      steps {
        build job: '../ci-plugin-snapshot/master'
      }
    }
    stage('Run test-runner') {
      agent {
        node {
          label "docker"
        }
      }
      environment {
        HOME = "${env.WORKSPACE}"
      }
      steps {
        script {
          println("Start scm-server using image ${imageTag}")
          docker.image(imageTag).withRun("--name scm-server -v ${env.WORKSPACE}/scm-home/init.script.d:/var/lib/scm/init.script.d -e NO_COLOR=1 -e JAVA_OPTS='-Dscm.initialPassword=scmadmin' -e TRP_PLUGINS=${params.Plugins}") {
            def ip = sh(script: "docker inspect -f \"{{.NetworkSettings.IPAddress}}\" scm-server", returnStdout: true).trim()
            docker.image('scmmanager/node-build:14.16.0').inside {
              withCredentials([usernamePassword(credentialsId: 'cesmarvin-github', passwordVariable: 'GITHUB_API_TOKEN', usernameVariable: 'GITHUB_ACCOUNT')]) {
                sh "LOG_LEVEL=${params.Log_Level} yarn integration-test-runner collect -c -s"
                sh "curl -X POST -u scmadmin:scmadmin \"http://${ip}:8080/scm/api/v2/plugins/available/scm-script-plugin/install?restart=true\""
                sh "LOG_LEVEL=${params.Log_Level} yarn integration-test-runner provision -a \"http://${ip}:8080/scm\" -u scmadmin -p scmadmin"
                sh "LOG_LEVEL=${params.Log_Level} yarn integration-test-runner run -a \"http://${ip}:8080/scm\" -u scmadmin -p scmadmin"
              }
            }
            sh "docker logs scm-server > scm-server.log"
          }
        }
      }
      post {
        failure {
          junit allowEmptyResults: true, testResults: "cypress/reports/*.xml"
          archiveArtifacts allowEmptyArchive: true, artifacts: "cypress/screenshots/**/*.png"
          archiveArtifacts allowEmptyArchive: true, artifacts: "cypress/videos/**/*.mp4"
          archiveArtifacts allowEmptyArchive: true, artifacts: "scm-server.log"
        }
      }
    }
  }

  post {
    failure {
      mail to: "scm-team@cloudogu.com",
        subject: "Jenkins Job ${JOB_NAME} - Build #${BUILD_NUMBER} - ${currentBuild.currentResult}!",
        body: "Check console output at ${BUILD_URL} to view the results."
    }
  }
}
String imageTag;
