# Docker file to create an environment for building/testing samples.
#
# To build new image use
#   docker build -t docker-registry.labs.intellij.net/kotlin-native/samples-environment:3 .
#
# To publish image to the JB internal registry use
#   docker push docker-registry.labs.intellij.net/kotlin-native/samples-environment:3

FROM ubuntu

ENV jdk_download_url=https://download.java.net/java/GA/jdk10/10.0.2/19aef61b38124481863b1413dce1855f/13/openjdk-10.0.2_linux-x64_bin.tar.gz
ENV jdk_install_path=/opt/jdk
ENV adt_download_url=https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip
ENV adt_platform="platforms;android-28"
ENV adt_build_tools="build-tools;28.0.3"

RUN apt-get -y update
RUN apt-get -y upgrade
RUN apt-get -y install sudo wget curl unzip cmake git libgit2-dev libcurl4-gnutls-dev libgtk-3-dev freeglut3-dev libsdl2-dev libavcodec-dev libavformat-dev libavutil-dev libswscale-dev libswresample-dev

WORKDIR /tmp

# Obtain the freshest CA certs bundle from the latest available JDK.
# Need it because newer CA certs will allow making SSL handshakes to AWS-hosted hosts such as https://downloads.jetbrains.com.
RUN apt-get -y install default-jdk
RUN update-alternatives --list java | sed 's/\/bin\/java$//' | xargs -I %% cp %%/lib/security/cacerts ./
RUN apt-get -y purge --auto-remove default-jdk

# Install non-LTS JDK:
RUN wget ${jdk_download_url}
RUN tar -xzf openjdk-*.tar.gz
RUN mv ./jdk* ${jdk_install_path}
# use newer CA certs:
RUN mv ${jdk_install_path}/lib/security/cacerts ${jdk_install_path}/lib/security/cacerts.orig
RUN cp ./cacerts ${jdk_install_path}/lib/security/
RUN update-alternatives --install /usr/bin/java java ${jdk_install_path}/bin/java 1
RUN update-alternatives --install /usr/bin/javac javac ${jdk_install_path}/bin/javac 1

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
