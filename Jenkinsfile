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
        node {
          label "scmm"
        }
      }
      environment {
        HOME = "${env.WORKSPACE}"
      }
      steps {
        resolveTag()
        script { currentBuild.description = "${imageTag}" }
      }
    }
    stage('Download plugins') {
      agent {
        node {
          label "scmm"
        }
      }
      environment {
        HOME = "${env.WORKSPACE}"
      }
      steps {
        preparePlugins()
      }
    }
    stage('Run test-runner') {
      agent {
        node {
          label "scmm"
        }
      }
      environment {
        HOME = "${env.WORKSPACE}"
      }
      steps {
        script {
          println("Start scm-server using image ${imageTag}")
          docker.image(imageTag).withRun("--name scm-server -v ${env.WORKSPACE}/plugin_downloads:/tmp/plugin_downloads -e JAVA_OPTS='-Dscm.initialPassword=scmadmin -Dscm.stage=TESTING'") {
            // We need to wait here because the plugins directory on the scm-server is not ready yet
            sh("sleep 90")
            sh("docker exec -i scm-server bash -c 'cp /tmp/plugin_downloads/*.smp /var/lib/scm/plugins'")
            sh("docker restart scm-server")
            sh("sleep 150")
            def ip = sh(script: "docker inspect -f \"{{.NetworkSettings.IPAddress}}\" scm-server", returnStdout: true).trim()
            docker.image('scmmanager/node-build:14.16.0').inside {
              withCredentials([usernamePassword(credentialsId: 'cesmarvin', passwordVariable: 'GITHUB_API_TOKEN', usernameVariable: 'GITHUB_ACCOUNT')]) {
                sh "yarn install"
                sh "yarn bin integration-test-runner"
                sh "LOG_LEVEL=${params.Log_Level} yarn integration-test-runner collect -c -s"
                sh "LOG_LEVEL=${params.Log_Level} yarn integration-test-runner provision -a \"http://${ip}:8080/scm\" -u scmadmin -p scmadmin"
                sh "NO_COLOR=1 LOG_LEVEL=${params.Log_Level} yarn integration-test-runner run -a \"http://${ip}:8080/scm\" -u scmadmin -p scmadmin"
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

void preparePlugins() {
  println("Download plugins")
  println(params.Plugins)
  String[] plugins = !params.Plugins.equals("") ? params.Plugins.split(",") : getDefaultPlugins()
  sh "rm -rf ~/plugin_downloads && mkdir ~/plugin_downloads"
  plugins.each { p ->
    sh("wget -O ${env.WORKSPACE}/plugin_downloads/${p}.smp https://packages.scm-manager.org/repository/latest_plugin_snapshots/${p}.smp")
  }
}

void resolveTag() {
  if (env.BRANCH_NAME.startsWith("release/")) {
    imageTag = "scmmanager/scm-manager:" + env.BRANCH_NAME.split("/", 2)[1]
  } else {
    sh("sudo apt-get install -y jq")
    def tagVersion = sh(script: "curl -L -N --fail 'https://hub.docker.com/v2/repositories/cloudogu/scm-manager/tags/?page_size=1' | jq '.results | .[] | .name' -r | sed 's/latest//'", returnStdout: true).trim()
    imageTag = "cloudogu/scm-manager:" + tagVersion
  }
}

String[] getDefaultPlugins() {
  return [
    "scm-repository-size-plugin",
    "scm-mustache-documentation-plugin",
    "scm-webhook-plugin",
    "scm-teamscale-plugin",
    "scm-branchwp-plugin",
    "scm-landingpage-plugin",
    "scm-script-plugin",
    "scm-repository-trash-bin-plugin",
    "scm-authormapping-plugin",
    "scm-gravatar-plugin",
    "scm-audit-log-plugin",
    "scm-groupmanager-plugin",
    "scm-ssh-plugin",
    "scm-metrics-json-plugin",
    "scm-tagprotection-plugin",
    "scm-repository-template-plugin",
    "scm-gotenberg-plugin",
    "scm-review-plugin",
    "scm-code-editor-plugin",
    "scm-file-lock-plugin",
    "scm-readme-plugin",
    "scm-jira-plugin",
    "scm-mail-plugin",
    "scm-binary-search-plugin",
    "scm-content-search-plugin",
    "scm-metrics-prometheus-plugin",
    "scm-notify-plugin",
    "scm-history-download-plugin",
    "scm-directfilelink-plugin",
    "scm-el-plugin",
    "scm-repository-mirror-plugin",
    "scm-sonar-plugin",
    "scm-commit-search-plugin",
    "scm-redmine-plugin",
    "scm-support-plugin",
    "scm-repository-size-plugin",
    "scm-argocd-plugin",
    "scm-markdown-plantuml-plugin",
    "scm-editor-plugin",
    "scm-statistic-plugin",
    "scm-issuetracker-plugin",
    "scm-pushlog-plugin",
    "scm-pathwp-plugin",
    "scm-repository-avatar-plugin",
    "scm-ssl-context-plugin",
    "scm-openapi-plugin",
    "scm-jenkins-plugin",
    "scm-commit-message-checker-plugin",
    "scm-ci-plugin",
    "scm-manage-folder-plugin",
    "scm-trace-monitor-plugin",
    "scm-archive-plugin",
    "scm-external-file-plugin"
  ]
}

String imageTag;
