@Library('lite-jenkins-pipeline') _
def slackChannels = [started: ['#lite-jenkins'], successful: ['#lite-jenkins'], failed: ['#lite-builds', '#lite-jenkins']]

node('jdk8') {
  slackBuildNotifier.notifyBuild("STARTED", slackChannels)
  try {

    stage('Clean workspace'){
      deleteDir()
    }

    stage('Checkout files'){
      checkout scm
    }

    stage('SBT test') {
      try {
        sh 'sbt test'
      }
      finally {
        step([$class: 'JUnitResultArchiver', testResults: 'target/test-reports/**/*.xml'])
      }
    }

  }
  catch (e) {
    currentBuild.result = "FAILED"
    throw e
  }
  finally {
    slackBuildNotifier.notifyBuild(currentBuild.result, slackChannels)
  }
}