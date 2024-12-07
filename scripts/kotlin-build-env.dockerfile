# Local build with no caches:
# docker build --no-cache -t local/kotlin-build-env:v7 -f kotlin-build-env.dockerfile .

FROM debian:11.8-slim

RUN apt-get update \
    && apt-get install -y locales \
    && localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
ENV LANG=en_US.utf8

RUN apt-get install -y git \
    && apt-get install -y curl \
    && apt-get install -y zip

RUN rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/lib/jvm

RUN curl https://cdn.azul.com/zulu/bin/zulu6.22.0.3-jdk6.0.119-linux_x64.tar.gz | tar -xz -C /usr/lib/jvm

RUN curl https://cdn.azul.com/zulu/bin/zulu7.40.0.15-ca-jdk7.0.272-linux_x64.tar.gz | tar -xz -C /usr/lib/jvm

RUN curl https://corretto.aws/downloads/resources/8.392.08.1/amazon-corretto-8.392.08.1-linux-x64.tar.gz | tar -xz -C /usr/lib/jvm

RUN curl https://cdn.azul.com/zulu/bin/zulu9.0.7.1-ca-jdk9.0.7-linux_x64.tar.gz | tar -xz -C /usr/lib/jvm

RUN curl https://corretto.aws/downloads/resources/11.0.21.9.1/amazon-corretto-11.0.21.9.1-linux-x64.tar.gz | tar -xz -C /usr/lib/jvm

RUN curl https://corretto.aws/downloads/resources/17.0.9.8.1/amazon-corretto-17.0.9.8.1-linux-x64.tar.gz | tar -xz -C /usr/lib/jvm

RUN curl https://corretto.aws/downloads/resources/21.0.1.12.1/amazon-corretto-21.0.1.12.1-linux-x64.tar.gz | tar -xz -C /usr/lib/jvm

# New naming conventions
ENV JDK6=/usr/lib/jvm/zulu6.22.0.3-jdk6.0.119-linux_x64 \
    JDK7=/usr/lib/jvm/zulu7.40.0.15-ca-jdk7.0.272-linux_x64 \
    JDK8=/usr/lib/jvm/amazon-corretto-8.392.08.1-linux-x64 \
    JDK9=/usr/lib/jvm/zulu9.0.7.1-jdk9.0.7-linux_x64 \
    JDK11=/usr/lib/jvm/amazon-corretto-11.0.21.9.1-linux-x64 \
    JDK17=/usr/lib/jvm/amazon-corretto-17.0.9.8.1-linux-x64 \
    JDK21=/usr/lib/jvm/amazon-corretto-21.0.1.12.1-linux-x64

# TeamCity JDK old naming conventions. Kotlin build still have dependencies in Maven build.
ENV JDK_18=$JDK8

ENV JDK_16_x64=$JDK6 \
    JDK_17_x64=$JDK7 \
    JDK_18_x64=$JDK8 \
    JDK_9_x64=$JDK9

ENV JDK_11_0=$JDK11 \
    JDK_17_0=$JDK17 \
    JDK_21_0=$JDK21

ENV JAVA_HOME=$JDK_11_0
ENV PATH="$PATH:$JAVA_HOME/bin"

RUN curl "https://archive.apache.org/dist/maven/maven-3/3.8.1/binaries/apache-maven-3.8.1-bin.tar.gz" | tar -xz -C /usr/lib

ENV M2_HOME=/usr/lib/apache-maven-3.8.1 \
    MAVEN_OPTS="-Xmx2G"

ENV MAVEN_HOME=$M2_HOME
ENV PATH="$PATH:$M2_HOME/bin"