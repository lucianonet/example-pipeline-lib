//import com.functions.ProvisionItems

/**
 * The step entry point.
 */
def call() {
   script { 
    String repositoryName = env.JOB_NAME.split('/')[1]
    String rootFolderPath = "Generated/$repositoryName"

    println rootFolderPath
    println env.GIT_URL
    
    List<String> jenkinsfilePaths = provisionItems(rootFolderPath, env.GIT_URL)
    
    //List<String> multibranchPipelinesToRun = findMultibranchPipelinesToRun(jenkinsfilePaths)
    //runPipelines(rootFolderPath, multibranchPipelinesToRun)
   } 
}


 List<String> provisionItems(String rootFolderPath, String repositoryURL) {
    // Find all Jenkinsfiles.
    List<String> jenkinsfilePaths = findFiles(glob: '**/*/Jenkinsfile').collect { it.path }

    // Provision folder and Multibranch Pipelines.
//    jobDsl(
//            scriptText: libraryResource('multiPipelines.groovy'),
//            additionalParameters: [
//                    jenkinsfilePathsStr: jenkinsfilePaths,
//                    rootFolderStr      : rootFolderPath,
//                    repositoryURL      : env.GIT_URL
//            ],
            // The following may be set to 'DELETE'. Note that branches will compete to delete and recreate items
            // unless you only provision items from the default branch.
//            removedJobAction: 'IGNORE'
//    )

    return jenkinsfilePaths
 }
