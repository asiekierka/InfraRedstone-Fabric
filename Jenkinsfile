pipeline {
	agent any
	stages {
		stage('Build') {
			steps {
				checkout scm
				sh 'rm -f private.gradle'
				sh './gradlew init clean build'
				archive 'build/libs/*jar'
			}
		}
		stage('Deploy') {
			steps {
				withCredentials([file(credentialsId: 'privateGradleNoSnapshotShadow', variable: 'PRIVATEGRADLE')]) {
					sh '''
						cp "$PRIVATEGRADLE" private.gradle
					'''
				}
			}
		}
	}
}
