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
      steps {
        sh "yarn install"
        script {
          imageTag = "cloudogu/scm-manager:" + sh(script: "node scripts/fetch-image-version.js", returnStdout: true)
        }
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
          docker.image("cloudogu/scm-manager:${imageTag}").withRun("--name scm-server") {
            docker.image('scmmanager/node-build:12.16.3').inside {
              // Test runner ausführen
              // scm-server:8080 für Verbindung
            }
          }
        }
      }
    }
  }
}
String imageTag;
