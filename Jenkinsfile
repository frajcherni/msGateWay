pipeline {
    agent any

    environment {
        DOCKER_HUB_USER = 'ahmedbelkahl'
        IMAGE_NAME = 'api-gateway'
        REGISTRY = "${DOCKER_HUB_USER}/${IMAGE_NAME}"
        SONARQUBE = 'sonarqube' 
        SONAR_LOGIN = 'squ_8f2f9c0cc9aa914b4fdbae3ca57f3c83d32f5df8' 
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE}") {
                    sh """
                        mvn sonar:sonar \
                        -Dsonar.projectKey=api-gateway \
                        -Dsonar.host.url=http://localhost:9000 \
                        -Dsonar.login=${SONAR_LOGIN}
                    """
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
            cleanWs()
        }
    }
}
