//import com.functions.provisionItems
import com.functions.functionUtils

/**
 * The step entry point.
 */
def call() {
    String repositoryName = env.JOB_NAME.split('/')[1]
    String rootFolderPath = "Generated/$repositoryName"

    println rootFolderPath
    println env.GIT_URL

    functionUtils fu = new functionUtils()
    
    fu.helloWorld()
    //List<String> jenkinsfilePaths = provisionItems(rootFolderPath, env.GIT_URL)
    
    //List<String> multibranchPipelinesToRun = findMultibranchPipelinesToRun(jenkinsfilePaths)
    //runPipelines(rootFolderPath, multibranchPipelinesToRun)
}
