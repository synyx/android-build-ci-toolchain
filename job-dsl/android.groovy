def generalJenkinsSSHKeyUUID =
def gitlabBaseUrl =
def gitlabUrl =
def jdkInstallation =
def mailingList =
// only needed when more than one installation is defined
def sonarInstallation =

def repos = [
        // Repo Name
        '<repoName>'           : [
                // Relative repository Path, e.g. mobile/test-repo
                repo:
                // Mail Notification to set addresses, can be changed for every Job
                mailer: mailingList,
                // Jenkins tab name
                view:
                // enable to test on Android Devices - boolean
                devicetest:
                // JaCoCo - boolean
                coverage:
                // String
                packageId: 
                // delete if no automatic deployment is wanted
                deployments: [
                        [
                                // Job Deploy Name
                                name:
                                apk:'release/app-release.apk'
                        ],
                ],
        ]
]

def jobViews = [:]

repos.each() {

    repoName, metaData ->

        def buildJobBaseName = "${repoName}"

        // Master Job
        def masterBuildJobName = "${buildJobBaseName}-master"
        def viewName = metaData.view
        if (jobViews[viewName] == null) {

            jobViews[viewName] = []
        }
        jobViews[viewName].add(masterBuildJobName)
        freeStyleJob(masterBuildJobName) {

            wrappers {
                androidEmulator {
                //setting a name will use an existing AVD by default.
                    avdName(null)
                    deviceDefinition(null)
                    osVersion('android-25')
                    screenDensity('120')
                    screenResolution('WVGA')
                    deviceLocale('en_US')
                    sdCardSize(null)
                    wipeData(false)
                    showWindow(false)
                    useSnapshots(false)
                    deleteAfterBuild(true)
                    startupDelay(5)
                    startupTimeout(0)
                    commandLineOptions('')
                    targetAbi('google_apis/x86')
                    executable('')
                    avdNameSuffix(null)
                }
            }

            label('android')
            properties {

                gitLabConnectionProperty {

                    gitLabConnection gitlabUrl
                }
            }
            jdk(jdkInstallation)
            logRotator {

                numToKeep(5)
            }
            scm {

                git {

                    remote {

                        name('origin')
                        url("${gitlabBaseUrl}${metaData.repo}")
                        credentials(generalJenkinsSSHKeyUUID)

                        refspec('+refs/heads/master:refs/remotes/origin/master')
                    }
                    branch('master')
                    extensions {

                        localBranch('master')
                    }
                }
            }
            triggers {

                gitlabPush {

                    buildOnMergeRequestEvents(false)
                    buildOnPushEvents(true)
                    enableCiSkip(false)
                    setBuildDescription(true)
                    rebuildOpenMergeRequest('never')
                    includeBranches('master')
                }
            }
            steps {

                gradle {

                    useWrapper(true)
                    fromRootBuildScriptDir(true)
                    tasks('clean build connectedAndroidTest')
                    }
                }
                if(metaData.coverage){

                    gradle{

                        useWrapper(true)
                        fromRootBuildScriptDir(true)
                        tasks('jacocoTestReport')
                    }
                }

                if(metaData.devicetest) {

                    monkeyBuilder {

                        filename(null)
                        packageId(metaData.packageId+'.debug')
                        eventCount(1000)
                        throttleMs(0)
                        seed('0')
                        categories(null)
                        extraParameters(null)
                    }
                    if(metaData.coverage) {

                        gradle {

                            useWrapper(true)
                            fromRootBuildScriptDir(true)
                            tasks('copyCoverageFile')
                        }
                    }
                }

                sonarRunnerBuilder {

                    jdk(jdkInstallation)
                    properties('')
                    installationName(sonarInstallation)
                    project('')
                    javaOpts('')
                    task('')
                    additionalArguments('')
                }
            }
            publishers {

                mailer(metaData.mailer)
            }
            wrappers {

              androidEmulator{

                avdName('Emulator')
                deleteAfterBuild(true)
                showWindow(false)
              }
                timeout {

                    absolute(60)
                }
                timestamps()
            }
        }

        // Deployment Jobs
        if(metaData.deployments){

            metaData.deployments.each {

                def deploymentJobName = it.name
                def apkname = it.apk
                jobViews[viewName].add(deploymentJobName)
                freeStyleJob(deploymentJobName) {

                    label('android')
                    properties {

                        gitLabConnectionProperty {

                            gitLabConnection gitlabUrl
                        }
                    }
                    jdk(jdkInstallation)
                    logRotator {

                        numToKeep(5)
                    }
                    scm {

                        git {

                            remote {

                                name('origin')
                                url("${gitlabBaseUrl}${metaData.repo}")
                                credentials(generalJenkinsSSHKeyUUID)

                                refspec('+refs/heads/master:refs/remotes/origin/master')
                            }
                            branch('master')
                            extensions {
                                localBranch('master')
                            }
                        }
                    }
                    triggers {

                        upstream(masterBuildJobName)
                    }
                    steps {

                        gradle {

                            useWrapper(true)
                            fromRootBuildScriptDir(true)
                            tasks('clean build')
                        }
                    }
                    publishers {

                        mailer(metaData.mailer)

                        androidApkUpload {

                            apkFilesPattern("app/build/outputs/apk/" + apkname)
                            googleCredentialsId('Google Play Android Developer')
                            trackName('alpha')
                        }
                    }
                    wrappers {

                        timeout {

                            absolute(60)
                        }
                        timestamps()
                    }
                }
            }

        }

        // Merge Request Job
        def mergeRequestBuildJobName = "${buildJobBaseName}-merge-request"
        jobViews[viewName].add(mergeRequestBuildJobName)
        freeStyleJob(mergeRequestBuildJobName) {

            label('android')
            properties {

                gitLabConnectionProperty {

                    gitLabConnection gitlabUrl
                }
            }
            jdk(jdkInstallation)
            logRotator {

                numToKeep(20)
            }
            scm {

                git {

                    remote {

                        name('origin')
                        url("${gitlabBaseUrl}${metaData.repo}")
                        credentials(generalJenkinsSSHKeyUUID)
                        refspec('+refs/heads/*:refs/remotes/origin/*')
                    }
                    branch('origin/${gitlabSourceBranch}')
                    extensions {

                        cleanCheckout()
                        mergeOptions {

                            branch('${gitlabTargetBranch}')
                            fastForwardMode(FastForwardMergeMode.FF)
                            remote('origin')
                            strategy('default')
                        }
                    }
                }
            }
            triggers {

                gitLabPushTrigger {

                    triggerOnMergeRequest(true)
                    triggerOnAcceptedMergeRequest(false)
                    triggerOnClosedMergeRequest(false)
                    triggerOnPush(false)
                    triggerOnNoteRequest(true)
                    triggerOpenMergeRequestOnPush('both')
                    noteRegex('Jenkins please retry a build')
                    ciSkip(true)
                    skipWorkInProgressMergeRequest(false)
                    setBuildDescription(true)
                    addNoteOnMergeRequest(true)
                    addCiMessage(true)
                    addVoteOnMergeRequest(true)
                    acceptMergeRequestOnSuccess(false)
                    branchFilterType('All')
                    includeBranchesSpec('')
                    excludeBranchesSpec('')
                    targetBranchRegex('')
                    mergeRequestLabelFilterConfig {

                        include('')
                        exclude('')
                    }
                    secretToken('')
                }
            }
            steps {

                gradle {

                    useWrapper(true)
                    fromRootBuildScriptDir(true)
                    tasks('clean build connectedAndroidTest')
                    }
                }
                if(metaData.devicetest) {

                    monkeyBuilder {

                        filename(null)
                        packageId(metaData.packageId)
                        eventCount(1000)
                        throttleMs(0)
                        seed('0')
                        categories(null)
                        extraParameters(null)
                    }
                }
            }
            publishers {

                gitLabCommitStatusPublisher {

                    name('jenkins')
                    markUnstableAsSuccess(false)
                }
                mailer(metaData.mailer)
            }
            wrappers {

                timeout {

                    absolute(60)
                }
                timestamps()
            }
        }
}

jobViews.each() {

    view, jobNames -> listView(view) {

            jobs {

                names(jobNames as String[])
            }
            columns {

                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
    }
}
