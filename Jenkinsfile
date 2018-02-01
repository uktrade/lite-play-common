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
  }
}