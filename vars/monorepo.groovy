import com.functions.ProvisionItems

/**
 * The step entry point.
 */
def call() {
   script { 
    String repositoryName = env.JOB_NAME.split('/')[1]
    String rootFolderPath = "Generated/$repositoryName"

    println rootFolderPath
    println env.GIT_URL

    ProvisionItems pi = new ProvisionItems()
    println "Ajunge aici"
    List<String> jenkinsfilePaths = pi.provisionItems(rootFolderPath, env.GIT_URL)
    
    //List<String> multibranchPipelinesToRun = findMultibranchPipelinesToRun(jenkinsfilePaths)
    //runPipelines(rootFolderPath, multibranchPipelinesToRun)
   } 
}
