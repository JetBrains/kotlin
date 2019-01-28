# Docker file to create an environment for building/testing samples.
#
# To build new image use
#   docker build -t docker-registry.labs.intellij.net/kotlin-native/samples-environment:3 .
#
# To publish image to the JB internal registry use
#   docker push docker-registry.labs.intellij.net/kotlin-native/samples-environment:3

FROM ubuntu

ENV adt_download_url=https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip
ENV adt_platform="platforms;android-28"
ENV adt_build_tools="build-tools;28.0.3"

RUN apt-get -y update
RUN apt-get -y upgrade
RUN apt-get -y install sudo wget curl unzip cmake git libgit2-dev libcurl4-gnutls-dev libgtk-3-dev freeglut3-dev libsdl2-dev libavcodec-dev libavformat-dev libavutil-dev libswscale-dev libswresample-dev

WORKDIR /tmp

# Install JDK 8:
RUN apt-get -y install openjdk-8-jdk

# Install Android SDK:
RUN wget ${adt_download_url}
RUN unzip sdk-tools-*.zip -d /opt/adt
RUN yes | /opt/adt/tools/bin/sdkmanager "${adt_platform}" "${adt_build_tools}"

# Install Python:
RUN apt-get -y install python-pip python-dev build-essential
RUN pip install --upgrade pip
RUN pip install --upgrade virtualenv
RUN pip -q install pyyaml typing

RUN rm -rf /tmp/*
WORKDIR /root
