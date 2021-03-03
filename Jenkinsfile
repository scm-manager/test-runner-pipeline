#!groovy
pipeline {
 options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds()
  }

  agent {
    docker {
      image 'scmmanager/java-build:11.0.9.1_1-2'
      args '-v /var/run/docker.sock:/var/run/docker.sock --group-add 998'
      label 'docker'
    }
  }




}