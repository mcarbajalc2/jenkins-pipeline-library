def call(Map config) {
    pipeline {
        agent any

        environment {
            IMAGE_NAME       = config.imageName
            REGISTRY         = config.registry ?: 'localhost:5000'
            HOST_PORT_PROD   = config.hostPortProd ?: '8080'
            HOST_PORT_DEV    = config.hostPortDev ?: '8081'
            CONTAINER_PORT   = config.containerPort ?: '3000'
            DOCKER_NETWORK   = config.dockerNetwork ?: 'infra_default'
        }

        stages {

            stage('Prepare') {
                steps {
                    script {
                        echo "Detected branch: ${env.BRANCH_NAME}"

                        if (env.BRANCH_NAME == 'main') {
                            env.TAG  = 'production'
                            env.APP_PORT = "${HOST_PORT_PROD}"
                            env.SUPABASE_API_PORT = '9012'
                            env.SUPABASE_STUDIO_PORT = '9014'
                            env.DB_PORT = '9016'
                        } else if (env.BRANCH_NAME == 'develop') {
                            env.TAG  = 'develop'
                            env.APP_PORT = "${HOST_PORT_DEV}"
                            env.SUPABASE_API_PORT = '9013'
                            env.SUPABASE_STUDIO_PORT = '9015'
                            env.DB_PORT = '9017'
                        } else {
                            error "Unsupported branch: ${env.BRANCH_NAME}"
                        }

                        env.FULL_IMAGE = "${REGISTRY}/${IMAGE_NAME}:${TAG}"

                        echo "Deploying: ${FULL_IMAGE} on host port ${env.APP_PORT} -> container port ${CONTAINER_PORT}"
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

            stage('Write env file') {
                steps {
                    script {
                        def envFileCredentialId = (env.TAG == 'production') ? 'env-production' : 'env-develop'
                        def envFileName = (env.TAG == 'production') ? '.env.production' : '.env.develop'

                        withCredentials([file(credentialsId: envFileCredentialId, variable: 'ENV_FILE')]) {
                            sh """
                            echo "Copying env file to workspace as ${envFileName}"
                            cp \$ENV_FILE ${envFileName}
                            """
                        }
                    }
                }
            }

            stage('Stop previous app container') {
                steps {
                    script {
                        echo "Stopping and removing previous app container if exists"
                        sh """
                        if [ \$(docker ps -q -f name=${IMAGE_NAME}-${TAG}) ]; then
                            docker stop ${IMAGE_NAME}-${TAG}
                            docker rm ${IMAGE_NAME}-${TAG}
                        fi
                        """
                    }
                }
            }

            stage('Deploy app & Supabase stack') {
                steps {
                    script {
                        def envFile = env.TAG == 'production' ? '.env.production' : '.env.develop'

                        echo "Deploying app and Supabase stack with ${envFile}"

                        sh """
                        docker compose --env-file ${envFile} up -d --build
                        """
                    }
                }
            }
        }

        post {
            failure {
                echo "❌ Pipeline failed for branch ${env.BRANCH_NAME}."
            }
            success {
                echo "✅ Pipeline succeeded for branch ${env.BRANCH_NAME} and deployed on port ${env.APP_PORT}."
            }
        }
    }
}
