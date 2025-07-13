def call(Map config) {
    pipeline {
        agent any

        environment {
            IMAGE_NAME      = config.imageName
            REGISTRY        = config.registry ?: 'localhost:5000'
            HOST_PORT_PROD  = config.hostPortProd ?: '8080'
            HOST_PORT_DEV   = config.hostPortDev ?: '8081'
            CONTAINER_PORT  = config.containerPort ?: '3000'
        }

        stages {

            stage('Prepare')  {
                steps {
                    script {
                        echo "Detected branch: ${env.BRANCH_NAME}"

                        if (env.BRANCH_NAME == 'main') {
                            env.TAG  = 'production'
                            env.PORT = env.HOST_PORT_PROD
                        } else if (env.BRANCH_NAME == 'develop') {
                            env.TAG  = 'develop'
                            env.PORT = env.HOST_PORT_DEV
                        } else {
                            error "Unsupported branch: ${env.BRANCH_NAME}"
                        }

                        env.FULL_IMAGE = "${env.REGISTRY}/${env.IMAGE_NAME}:${env.TAG}"

                        echo "Deploying: ${env.FULL_IMAGE} on host port ${env.PORT} -> container port ${env.CONTAINER_PORT}"
                    }
                }
            }

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Build & push') {
                steps {
                    sh """
                    echo "Building Docker image ${FULL_IMAGE}"
                    docker build -t ${FULL_IMAGE} .
                    echo "Pushing Docker image ${FULL_IMAGE}"
                    docker push ${FULL_IMAGE}
                    """
                }
            }

            stage('Deploy') {
                steps {
                    sh """
                    echo  "Stopping existing container if exists"
                    if [ \$(docker ps -q -f name=${IMAGE_NAME}-${TAG}) ]; then
                        docker stop ${IMAGE_NAME}-${TAG}
                        docker rm ${IMAGE_NAME}-${TAG}
                    fi

                    echo "Pulling latest image from registry"
                    docker pull ${FULL_IMAGE}

                    echo "Running new container"
                    docker run -d --name ${IMAGE_NAME}-${TAG} -p ${PORT}:${CONTAINER_PORT} ${FULL_IMAGE}
                    """
                }
            }

        }

        post {
            failure {
                echo "❌ Pipeline failed for branch ${env.BRANCH_NAME}."
            }
            success {
                echo "✅ Pipeline succeeded for branch ${env.BRANCH_NAME} and deployed on port ${env.PORT}."
            }
        }
    }
}
