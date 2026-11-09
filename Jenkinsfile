pipeline {
    agent any

    environment {
        DOCKER_HUB_USER = 'ahmedbelkahl'
        IMAGE_NAME = 'api-gateway'
        REGISTRY = "${DOCKER_HUB_USER}/${IMAGE_NAME}"
        // Nom du serveur SonarQube configuré dans Jenkins (Administrer Jenkins > Système).
        // Le token est stocké dans la configuration de ce serveur, jamais dans le code.
        SONARQUBE = 'sonarqube'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn -B clean verify'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                // withSonarQubeEnv fournit l'URL et le token (SONAR_HOST_URL / SONAR_AUTH_TOKEN)
                withSonarQubeEnv("${SONARQUBE}") {
                    sh 'mvn -B sonar:sonar -Dsonar.projectKey=api-gateway'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    docker.withRegistry('', 'docker-hub-credentials') {
                        def app = docker.build("${REGISTRY}:${env.BUILD_NUMBER}")
                        app.push()
                        app.push("latest")
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            cleanWs()
        }
    }
}
