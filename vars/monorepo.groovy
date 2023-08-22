//import com.functions.ProvisionItems
import org.jenkinsci.plugins.scriptsecurity.scripts.*
import java.nio.file.Path
import java.nio.file.Paths

/**
 * The step entry point.
 */
def call() {
   script { 
    String repositoryName = env.JOB_NAME.split('/')[1]
    String rootFolderPath = "Monorepos/$repositoryName"

    println rootFolderPath
    println env.GIT_URL
    
    printPendingScripts()
    
    
    List<String> jenkinsfilePaths = provisionItems(rootFolderPath, env.GIT_URL)
    jenkinsfilePaths.each { item ->
      println item
    }    

    List<String> multibranchPipelinesToRun = findMultibranchPipelinesToRun(jenkinsfilePaths)
    multibranchPipelinesToRun.each {item -> 
      println item
    }
    runPipelines(rootFolderPath, multibranchPipelinesToRun)
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
    println "ENV.IS_PR:" + env.IS_PR
    if (env.IS_PR=='true') {
        env.CHANGE_TARGET
    } else {
        [env.GIT_PREVIOUS_SUCCESSFUL_COMMIT, env.GIT_PREVIOUS_COMMIT]
        // Look for the first existing existing revision. Commits can be removed (e.g. with a `git push --force`), so a
        // previous build revision may not exist anymore.
                .find { revision ->
                    if (revision != null) {
                        def exitCode = sh(script: "git rev-parse --quiet --verify $revision", returnStdout: true)
                        println "SH cmd (git rev-parse --quiet --verify) exit code:" + exitCode
                    }
                    revision != null && sh(script: "git rev-parse --quiet --verify $revision", returnStdout: true) == 0
                } ?: 'HEAD^'
    }
}

/**
 * Run pipelines.
 * @param rootFolderPath The common root folder of Multibranch Pipelines.
 * @param multibranchPipelinesToRun The list of Multibranch Pipelines for which a Pipeline is run.
 */
def runPipelines(String rootFolderPath, List<String> multibranchPipelinesToRun) {
    
    
    
    parallel(multibranchPipelinesToRun.inject([:]) { stages, multibranchPipelineToRun ->
        stages + [("Build $multibranchPipelineToRun"): {
            println "Root Folder: " + rootFolderPath
            println "Multibranch pipepile to run: " + multibranchPipelineToRun
           //env.CHANGE_BRANCH
            println "UrlEncode: " + URLEncoder.encode(env.GIT_BRANCH, 'UTF-8')

            
            def pipelineName = "$rootFolderPath/$multibranchPipelineToRun/${URLEncoder.encode(env.GIT_BRANCH, 'UTF-8')}"
            // For new branches, Jenkins will receive an event from the version control system to provision the
            // corresponding Pipeline under the Multibranch Pipeline item. We have to wait for Jenkins to process the
            // event so a build can be triggered.
            println pipelineName
            timeout(time: 15, unit: 'MINUTES') {
                waitUntil(initialRecurrencePeriod: 1e3) {
                    def pipeline = Jenkins.instance.getItemByFullName(pipelineName)
                    pipeline && !pipeline.isDisabled()
                }
            }

            println "Multibranch pipepile to run: " + multibranchPipelineToRun

            // Trigger downstream builds.
            build(job: pipelineName, parameters: [string(name: 'project', value: multibranchPipelineToRun)],   propagate: true, wait: true)
        }]
    })
}
