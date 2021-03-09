#!groovy
pipeline {

  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds()
  }

  parameters {
    string(defaultValue: '', description: 'List of to be installed plugins (comma-separated)', name: 'Plugins', trim: true)
  }

  agent none

  triggers {
    cron('H H(3-5) * * 1-5')
  }

  stages {
    stage('Get version') {
      agent {
        docker {
          image 'scmmanager/node-build:12.16.3'
          label 'docker'
        }
      }
      environment {
        HOME = "${env.WORKSPACE}"
      }
      steps {
        sh "yarn install"
        script {
          def tagVersion = sh(script: "node scripts/fetch-image-version.js", returnStdout: true)
          imageTag = "cloudogu/scm-manager:" + tagVersion
        }
      }
    }
    stage('Trigger ci-plugin-snapshot build') {
      steps {
        build job: '../ci-plugin-snapshot/master'
      }
    }
    stage('Run integration-test-runner') {
      agent {
        node {
          label "docker"
        }
      }
      environment {
        HOME = "${env.WORKSPACE}"
      }
      steps {
        sh "mkdir -p integration-test-runner/cypress/screenshots/"
        sh "mkdir -p integration-test-runner/cypress/videos/"
        script {
          println("Start scm-server using image ${imageTag}")
          docker.image(imageTag).withRun("--name scm-server -v ${env.WORKSPACE}/scm-home/init.script.d:/var/lib/scm/init.script.d -e TRP_PLUGINS=${params.Plugins}") {
            def ip = sh(script: "docker inspect -f \"{{.NetworkSettings.IPAddress}}\" scm-server", returnStdout: true).trim()
            docker.image('scmmanager/node-build:14.16.0').inside {
              withCredentials([usernamePassword(credentialsId: 'cesmarvin-github', passwordVariable: 'GITHUB_API_TOKEN', usernameVariable: 'GITHUB_ACCOUNT')]) {
                sh "LOG_LEVEL=debug SERVER_URL=\"http://${ip}:8080/scm\" ./scripts/run-integration-tests.sh"
              }
            }
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: "integration-test-runner/cypress/reports/*.xml"
          archiveArtifacts allowEmptyArchive: true, artifacts: "integration-test-runner/cypress/screenshots/**/*.png"
          archiveArtifacts allowEmptyArchive: true, artifacts: "integration-test-runner/cypress/videos/**/*.mp4"
        }
      }
    }
  }

  post {
    failure {
      //TODO change email to scm-team@cloudogu.com
      mail to: "eduard.heimbuch@cloudogu.com",
        subject: "Jenkins Job ${JOB_NAME} - Build #${BUILD_NUMBER} - ${currentBuild.currentResult}!",
        body: "Check console output at ${BUILD_URL} to view the results."
    }
  }
}

String imageTag;
