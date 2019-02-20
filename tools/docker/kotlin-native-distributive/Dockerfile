FROM ubuntu

ENV konan_data_dir=/opt/konan
ENV gradle_version=5.1
ENV gradle_install_path=/opt/gradle-${gradle_version}

RUN apt-get -y update
RUN apt-get -y upgrade
RUN apt-get -y install wget unzip

RUN mkdir /tmp/kotlin-native-bootstrap
WORKDIR /tmp/kotlin-native-bootstrap

# Install JDK 8:
RUN apt-get -y install openjdk-8-jdk

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
