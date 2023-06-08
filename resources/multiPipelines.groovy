
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Generate Multibranch Pipelines.
 * @param repositoryURL The Git repository URL.
 * @param jenkinsfilePaths A list of Jenkinsfiles paths.
 */
def generateMultibranchPipelines(List<Path> jenkinsfilePaths, Path rootFolder, String repositoryURL) {
    // The following variables are needed to configure the branch source for GitHub. Configuration for other version
    // control providers vary.
    def matcher = repositoryURL =~ /.+[\/:](?<owner>[^\/]+)\/(?<repository>[^\/]+)\.git$/
    matcher.matches()
    String repositoryOwner = matcher.group('owner')
    String repositoryName = matcher.group('repository')

    // Discover branches strategies
    final int EXCLUDE_PULL_REQUESTS_STRATEGY_ID = 1

    // Discover pull requests from origin strategies
    final int USE_CURRENT_SOURCE_STRATEGY_ID = 2

    jenkinsfilePaths.each { jenkinsfilePath ->
        String pipelineName = jenkinsfilePath.parent

        multibranchPipelineJob(rootFolder.resolve(pipelineName).toString()) {
            displayName(pipelineName)
            branchSources {
                branchSource {
                    source {
                        bitbucket {
                            // We must set a branch source ID.
                            //id('bitbucket')

                            repoOwner(repositoryOwner)
                            repository('Setarea asta nu functioneaza')
                            serverUrl('https://bitbucket.org')

                            // Make sure to properly set this.
                            credentialsId('jenkins-bitbucket')

                            traits {
                                  webhookRegistrationTrait {
		                    mode('ITEM')
		                  }
		    
		                  submoduleOptionTrait {
		                    extension {
		                      disableSubmodules(false)
		                      recursiveSubmodules(true)
		                      trackingSubmodules(false)
		                      reference(null)
		                      timeout(null)
		                      parentCredentials(true)
		                    }
		                  }
		    
		                  bitbucketBranchDiscovery {
		                    strategyId(1)
		                  }
		                  
		                  bitbucketPullRequestDiscovery {
		                    strategyId(1)
		                  }
                            }
                        }
                    }

                    // By default, Jenkins will trigger builds as it detects changes on the source repository. We want
                    // to avoid that since we will trigger child pipelines on our own only when relevant.
                    buildStrategies {
                        skipInitialBuildOnFirstBranchIndexing()
                    }
                    
                    strategy {
                        allBranchesSame {
                            props {
                                suppressAutomaticTriggering {
                                    strategy('EVENTS') 
                                }
                            }
                        }
                    }

                }
            }
            factory {
                workflowBranchProjectFactory {
                    scriptPath(jenkinsfilePath.toString())
                }
            }
            orphanedItemStrategy {
                discardOldItems {
                    // Keeping pipelines a few days for branches that do not exist anymore can be useful for
                    // troubleshooting purposes.
                    daysToKeep(-1)
                    numToKeep(-1) 
                }
            }
            triggers {
                periodicFolderTrigger {
                    // Scan branches once a week at least to remove orphan pipelines.
                    interval('7d')
                }
            }
        }
    }
}


/**
 * Get all ancestor folders paths.
 * <pre>
 *     ancestorFoldersPath(Paths.get('root/parent/child/file'))
 *     // returns [root, root/parent, root/parent/child]
 * </pre>
 * @param path A path.
 * @return All its ancestors paths.
 */
List<Path> ancestorFoldersPath(Path path) {
    if (path.parent == null) return []
    ancestorFoldersPath(path.parent) + [path.parent]
}

/**
 * Generate folders.
 * @param jenkinsfilePaths A list of Jenkinsfile paths.
 */
def generateFolders(List<Path> jenkinsfilePaths, Path rootFolder) {
    jenkinsfilePaths
            .collect { ancestorFoldersPath(rootFolder.resolve(it).parent) }
            .flatten()
    // Remove duplicates in case some Jenkinsfiles share ancestors for optimization.
            .unique()
            .each {
                // Effectively provision the folder.
                folder(it.toString())
            }
}




// `jenkinsfilePathsStr` and `rootFolderStr` are global variables that are set through `jobDsl`'s `additionalParameters`
// options.
List<Path> jenkinsfilePaths = jenkinsfilePathsStr.collect { Paths.get(it) }
Path rootFolder = Paths.get(rootFolderStr)

generateFolders(jenkinsfilePaths, rootFolder)
generateMultibranchPipelines(jenkinsfilePaths, rootFolder, repositoryURL)
