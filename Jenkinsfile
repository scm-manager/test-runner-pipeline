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

  environment {
    HOME = "${env.workspace}"
  }

  stages {
    stage('Get version') {
      agent {
        docker {
          image 'scmmanager/node-build:12.16.3'
          label 'docker'
        }
      }
      steps {
        sh "yarn install"
        script {
          def tagVersion = sh(script: "node scripts/fetch-image-version.js", returnStdout: true)
          imageTag = "cloudogu/scm-manager:" + tagVersion
        }
      }
    }
    stage('Prepare scm-home') {
      agent {
        node {
          label "docker"
        }
      }
      when {
        expression {
          params.Plugins != ""
        }
      }
      steps {
        writeFile file: 'scm-home/init.script.d/plugins', text: params.Plugins
        sh 'cat scm-home/init.script.d/plugins.txt'
      }
    }
    stage('Start SCM Server') {
      agent {
        node {
          label "docker"
        }
      }
      steps {
        script {
          println("Start scm-server using image ${imageTag}")
          docker.image(imageTag).withRun("--name scm-server -v scm-home:/var/lib/scm -e TRP_PLUGINS=${params.Plugins}") {
            docker.image('scmmanager/node-build:12.16.3').inside {
              withCredentials([usernamePassword(credentialsId: 'cesmarvin-github', passwordVariable: 'GITHUB_API_TOKEN', usernameVariable: 'GITHUB_ACCOUNT')]) {
                sh "LOG_LEVEL=debug ./scripts/run-integration-tests.sh"
              }
            }
          }
        }
      }
    }
  }
}
String imageTag;
