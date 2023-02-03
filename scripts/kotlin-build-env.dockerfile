# Local build with no caches:
# docker build --no-cache -t local/kotlin-build-env:v6 -f kotlin-build-env.dockerfile .

FROM debian:10.12-slim

RUN apt-get update \
    && apt-get install -y locales \
    && localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
ENV LANG en_US.utf8

RUN apt-get install -y git \
    && apt-get install -y curl \
    && apt-get install -y zip

RUN rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/lib/jvm

RUN curl https://cdn.azul.com/zulu/bin/zulu6.22.0.3-jdk6.0.119-linux_x64.tar.gz | tar -xz -C /usr/lib/jvm

RUN curl https://cdn.azul.com/zulu/bin/zulu7.40.0.15-ca-jdk7.0.272-linux_x64.tar.gz | tar -xz -C /usr/lib/jvm

RUN curl https://corretto.aws/downloads/resources/8.322.06.2/amazon-corretto-8.322.06.2-linux-x64.tar.gz | tar -xz -C /usr/lib/jvm

RUN curl https://cdn.azul.com/zulu/bin/zulu9.0.7.1-ca-jdk9.0.7-linux_x64.tar.gz | tar -xz -C /usr/lib/jvm

# New naming conventions
ENV JDK6=/usr/lib/jvm/zulu6.22.0.3-jdk6.0.119-linux_x64 \
    JDK7=/usr/lib/jvm/zulu7.40.0.15-ca-jdk7.0.272-linux_x64 \
    JDK8=/usr/lib/jvm/amazon-corretto-8.322.06.2-linux-x64 \
    JDK9=/usr/lib/jvm/zulu9.0.7.1-jdk9.0.7-linux_x64

# TeamCity JDK old naming conventions. Kotlin build still have dependencies in Maven build.
ENV JDK_18=$JDK8

ENV JDK_16_x64=$JDK6 \
    JDK_17_x64=$JDK7 \
    JDK_18_x64=$JDK8 \
    JDK_9_x64=$JDK9

ENV JAVA_HOME=$JDK8 \
    PATH="$PATH:$JDK8/bin"

RUN curl "https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz" | tar -xz -C /usr/lib

ENV M2_HOME=/usr/lib/apache-maven-3.6.3 \
    MAVEN_OPTS="-Xmx2G"

ENV MAVEN_HOME=$M2_HOME \
    PATH="$PATH:$M2_HOME/bin"