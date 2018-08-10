def project = 
def packageIdName = 
def branchName = 'master'
def jobName = "${project}-${branchName}".replaceAll('/','-')
job(jobName) {
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
  scm {
    git {
      branch(branchName)
      remote {
        url("git://github.com/${project}.git")
      }
      extensions {}
    }
  }
  steps {
    gradle {
    tasks('clean build')
    }
    installBuilder {
      apkFile('app/build/outputs/apk/app-debug.apk')
      uninstallFirst(true)
      failOnInstallFailure(true)
    }
    gradle {
      tasks('connectedAndroidTest')
      monkeyBuilder {
        filename(null)
        packageId(packageIdName)
        eventCount(1000)
        throttleMs(0)
        seed('0')
        categories(null)
        extraParameters(null)
      }
    }
  }
}