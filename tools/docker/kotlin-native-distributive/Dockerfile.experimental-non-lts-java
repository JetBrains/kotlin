FROM ubuntu

ENV jdk_download_url=https://download.java.net/java/GA/jdk10/10.0.2/19aef61b38124481863b1413dce1855f/13/openjdk-10.0.2_linux-x64_bin.tar.gz
ENV jdk_install_path=/opt/jdk
ENV konan_data_dir=/opt/konan
ENV gradle_version=5.1
ENV gradle_install_path=/opt/gradle-${gradle_version}

RUN apt-get -y update
RUN apt-get -y upgrade
RUN apt-get -y install wget unzip

RUN mkdir /tmp/kotlin-native-bootstrap
WORKDIR /tmp/kotlin-native-bootstrap

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

# Install Gradle:
RUN wget https://services.gradle.org/distributions/gradle-${gradle_version}-bin.zip
RUN unzip ./gradle-*.zip
RUN mv ./gradle-${gradle_version} ${gradle_install_path}

# Install Kotlin/Native through the bootstrap Gradle project:
COPY ./build.gradle.kts ./
RUN mkdir ${konan_data_dir}
RUN KONAN_DATA_DIR=${konan_data_dir} ${gradle_install_path}/bin/gradle model

# For login shells (ex: docker attach <hash>):
RUN echo '#!/bin/sh' > /etc/profile.d/konan.sh
RUN echo "export KONAN_DATA_DIR=${konan_data_dir}" >> /etc/profile.d/konan.sh
RUN echo "export PATH=\$PATH:`ls -1d ${konan_data_dir}/kotlin-native-*/ | head -n 1`bin" >> /etc/profile.d/konan.sh
RUN chmod +x /etc/profile.d/konan.sh

# For non-login shells (ex: docker exec -it <hash> bash):
RUN echo '. /etc/profile.d/konan.sh' > /root/.bashrc

RUN rm -rf /tmp/*
WORKDIR /root

ENTRYPOINT /bin/bash
