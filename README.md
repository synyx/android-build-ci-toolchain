# Android Build and CI Toolchain for Jenkins

Setup for a Build and Continuous Integration Toolchain for Android Projects using Jenkins.  
This setup is using a dedicated Build Node in a Jenkins Cluster for better performance and priorization. Sonar and JaCoCo integration is included in the Jenkins Job file.
Publishing the .apk on the Play Store is automatically done for the Alpha track of the project.  

## Docker Setup
You can build the stack yourself with docker. To start the container use:

  ~~~sh
  # -p 50000:50000 if it's part of a cluster
  # -v jenkins_home:/var/jenkins_home to make the jenkins home dir persistent
  # --privileged -v /dev/bus/usb:/dev/bus/usb This shares the ADB server container's network with ADB client containers
  $ docker run --privileged -p 8080:8080 -p 50000:50000 -v jenkins_home:/var/jenkins_home -v /dev/bus/usb:/dev/bus/usb jenkins-android-ci
  ~~~

Go to [localhost:8080](http://localhost:8080) and paste the admin key to start the setup. Install any additional plugins via the GUI or add them to the last line of the `Dockerfile`.  

Out of the box you can add and manage jobs using Jenkins Job DSL. See the example [file](job-dsl/android.groovy).

The SDK will additionally install `platform-tools`, `build-tools;28.0.2` and `emulator`.

The Google Play Publisher needs to be configured manually, see their [documentation](https://plugins.jenkins.io/google-play-android-publisher).  

## Manual Setup

For this setup a dedicated Node was used, but it can easily be done on the Jenkins Master.  
The required plugins can be found [here](#plugins).

### SDK

Download the latest SDK tools [here](https://developer.android.com/studio/#downloads)

  ~~~sh
  cd /opt
  $ sudo wget ANDROID_SDK.zip
  $ sudo unzip ANDROID_SDK.zip -d android-sdk
  $ sudo chown -R JENKINS_USER:JENKINS_USER android-sdk
  ~~~

### Node Configuration:  

If a dedicated node is used for Android projects, add an 'android' label and set the usage to exclusive.  
Set `ANDROID_HOME` as an environment variable pointing to your Android SDK.  

![Node Configuration](docs/node-config.png)
 
## Plugins

[Android Emulator](https://plugins.jenkins.io/android-emulator)  

[Google Play Publisher](https://plugins.jenkins.io/google-play-android-publisher)  

[Job DSL](https://plugins.jenkins.io/job-dsl)  

[Self-Organizing Swarm Modules](https://plugins.jenkins.io/swarm)  

## Job Configuration

Jobs can easily be  created using the DSL Plugin.
A simple configuration with a github repo can be found in `github.groovy`.
Our configuration can be found in `android.groovy`.  
The following variables need to be set:

  ~~~ groovy
  generalJenkinsSSHKeyUUID  
  gitlabBaseUrl  
  gitlabUrl  
  jdkInstallation // JDK Installation, e.g. Oracle JDK 8. Only needed with multiple versions.  
  mailingList // all mail adresses that shall receive a notification.  
  sonarInstallation //Name of the Sonar Installation Instace used by Jenkins for this project.  
  ~~~

Repositories are added using the repos array.  
Example: 

  ~~~ groovy
  def repos = [
          'example-app'           : [
                  repo: 'mobile/ex-android',
                  mailer: mailingList,
                  view: 'android',
                  devicetest: true,
                  coverage: true,
                  packageId: 'de.synyx.mobile.ex',
                  deployments: [
                          [
                                  name:'ex-app-prod-deploy',
                                  apk:'release/app-release.apk'
                          ],
                  ],
          ]
          'example-app2'          : [
                  ...
  ]
  ~~~

For a detailed documentation of the configuration, see: [Job DSL Doc](https://jenkinsci.github.io/job-dsl-plugin)

Create a new item in Jenkins as a Freestyle project and add `Process Job DSLs` in the `Build` step. Running this will then create the jobs.

### Jobs explained

For each repo there will be three jobs created.
Master: Built on Push events on master branch with `clean build connectedAndroidTest [jacocoTestReport copyCoverageFile monkey]`, including AVD.
Deployment: If deployments are enabled, the apk will be built and uploaded with the configured Google Account on the Alpha track.
Merge Request: On Merge or Note Request will `clean build connectedAndroidTest [monkey]` the application. A jenkins user on GitLab will commit the status.


## Configuring AVD

This requires the aforementioned Android Emulator Plugin.  

By default the job will create a parameterized AVD during its runtime and destroyed afterwards. See [below](#avd-parameter) for the configuration.  
Possible configurations include using different AVDs for different jobs and reusing them.  

### AVD Parameter

  ~~~ groovy
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
  ~~~

### Known Problems
Currently kvm is not assigned a group and needs to be manually set to emulate x86 and x86_64.

  ~~~sh
  $ docker exec -u root -it <docker container> chgrp jenkins /dev/kvm
  ~~~

## ToDo

* Add Sonar to the Stack
* Labels in Docker
* undo deleting image
* Alpine
