pipeline {
  agent {
    node {
      label 'docker.ci.uktrade.io'
    }
  }
  stages {
    stage('prep') {
      steps {
        script {
          deleteDir()
          checkout scm
          deployer = docker.image("ukti/lite-image-builder")
          deployer.pull()
        }
      }
    }
    stage('test') {
      steps {
        script {
          deployer.inside {
            try {
              sh 'sbt -no-colors test'
            }
            finally {
              step([$class: 'JUnitResultArchiver', testResults: 'target/test-reports/**/*.xml'])
            }
          }
        }
      }
    }
    stage('sonarqube') {
      steps {
        script {
          deployer.inside {
            withSonarQubeEnv('sonarqube') {
              sh 'sbt -no-colors compile test:compile'
              sh "${env.SONAR_SCANNER_PATH}/sonar-scanner"
            }
          }
        }
      }
    }
  }
}