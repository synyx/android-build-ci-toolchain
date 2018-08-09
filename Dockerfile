FROM jenkins/jenkins:lts

ENV ANDROID_SDK_ZIP sdk-tools-linux-4333796.zip
ENV ANDROID_SDK_ZIP_URL https://dl.google.com/android/repository/$ANDROID_SDK_ZIP
ENV ANDROID_HOME /opt/android-sdk-linux
ENV PATH $PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/tools/bin

USER root

RUN apt-get update \
	&& apt-get install --no-install-recommends software-properties-common unzip -y \
	&& apt-get clean \
	&& rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* \
# Download the Android SDK
	&& curl -o /opt/$ANDROID_SDK_ZIP $ANDROID_SDK_ZIP_URL \
	&& unzip /opt/$ANDROID_SDK_ZIP -d /opt/android-sdk-linux \
	&& rm /opt/$ANDROID_SDK_ZIP \
# Install required tools
	&& yes | sdkmanager --licenses \
	&& sdkmanager "platform-tools" "build-tools;28.0.2" "emulator" \
    && chown -R jenkins:jenkins $ANDROID_HOME 

USER jenkins

RUN /usr/local/bin/install-plugins.sh git gradle job-dsl android-emulator google-play-android-publisher 