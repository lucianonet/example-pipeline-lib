//import com.functions.provisionItems
import com.functions.helloWorldddddd

/**
 * The step entry point.
 */
def call() {
    String repositoryName = env.JOB_NAME.split('/')[1]
    String rootFolderPath = "Generated/$repositoryName"

    println rootFolderPath
    println env.GIT_URL
    
    helloWorld()
    //List<String> jenkinsfilePaths = provisionItems(rootFolderPath, env.GIT_URL)
    
    //List<String> multibranchPipelinesToRun = findMultibranchPipelinesToRun(jenkinsfilePaths)
    //runPipelines(rootFolderPath, multibranchPipelinesToRun)
}
