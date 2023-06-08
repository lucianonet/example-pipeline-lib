//import com.functions.ProvisionItems
import org.jenkinsci.plugins.scriptsecurity.scripts.*

/**
 * The step entry point.
 */
def call() {
   script { 
    String repositoryName = env.JOB_NAME.split('/')[1]
    String rootFolderPath = "Generated/$repositoryName"

    println rootFolderPath
    println env.GIT_URL
    
    printPendingScripts()
    
    
    List<String> jenkinsfilePaths = provisionItems(rootFolderPath, env.GIT_URL)
    jenkinsfilePaths.each { item ->
      println item
    }    

    List<String> multibranchPipelinesToRun = findMultibranchPipelinesToRun(jenkinsfilePaths)
    //runPipelines(rootFolderPath, multibranchPipelinesToRun)
   } 
}
 
 @NonCPS
 void printPendingScripts() {
    ScriptApproval sa = ScriptApproval.get();
    //list pending approvals
    for (ScriptApproval.PendingScript pending : sa.getPendingScripts()) {
        println "Pending Script hash: " + pending.hash
        println "Pending Approved : " + pending.script
    }
 }

 List<String> provisionItems(String rootFolderPath, String repositoryURL) {
    // Find all Jenkinsfiles.
    List<String> jenkinsfilePaths = findFiles(glob: '**/*/Jenkinsfile').collect { it.path }

    // Provision folder and Multibranch Pipelines.
    jobDsl(
            scriptText: libraryResource('multiPipelines.groovy'),
            additionalParameters: [
                    jenkinsfilePathsStr: jenkinsfilePaths,
                    rootFolderStr      : rootFolderPath,
                    repositoryURL      : env.GIT_URL
            ],
            // The following may be set to 'DELETE'. Note that branches will compete to delete and recreate items
            // unless you only provision items from the default branch.
            removedJobAction: 'IGNORE'
    )

    return jenkinsfilePaths
 }


List<String> findMultibranchPipelinesToRun(List<String> jenkinsfilePaths) {
    findRelevantMultibranchPipelines(getChangedDirectories(getBaselineRevision()), jenkinsfilePaths)
}

@NonCPS
static List<String> findRelevantMultibranchPipelines(List<String> changedFilesPathStr, List<String> jenkinsfilePathsStr) {
    List<Path> changedFilesPath = changedFilesPathStr.collect { Paths.get(it) }
    List<Path> jenkinsfilePaths = jenkinsfilePathsStr.collect { Paths.get(it) }

    changedFilesPath.inject([]) { pipelines, changedFilePath ->
        def matchingJenkinsfile = jenkinsfilePaths
                .find { jenkinsfilePath -> changedFilePath.startsWith(jenkinsfilePath.parent) }
        matchingJenkinsfile != null ? pipelines + [matchingJenkinsfile.parent.toString()] : pipelines
    }.unique()
}

List<String> getChangedDirectories(String baselineRevision) {
    // Jenkins native interface to retrieve changes, i.e. `currentBuild.changeSets`, returns an empty list for newly
    // created branches (see https://issues.jenkins.io/browse/JENKINS-14138), so let's use `git` instead.
    sh(
            label: 'List changed directories',
            script: "git diff --name-only $baselineRevision | xargs -L1 dirname | uniq",
            returnStdout: true,
    ).split().toList()
}


String getBaselineRevision() {
    // Depending on your seed pipeline configuration and preferences, you can set the baseline revision to a target
    // branch, e.g. the repository's default branch or even `env.CHANGE_TARGET` if Jenkins is configured to discover
    // pull requests.
    [env.GIT_PREVIOUS_SUCCESSFUL_COMMIT, env.GIT_PREVIOUS_COMMIT]
    // Look for the first existing existing revision. Commits can be removed (e.g. with a `git push --force`), so a
    // previous build revision may not exist anymore.
            .find { revision ->
                revision != null && sh(script: "git rev-parse --quiet --verify $revision", returnStdout: true) == 0
            } ?: 'HEAD^'
}



