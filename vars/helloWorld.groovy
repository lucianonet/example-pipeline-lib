def call() {
    String repositoryName = env.JOB_NAME.split('/')[1]
    sh "echo ${repositoryName}"
    sh "echo Hello World. Today is Sunday"
}
