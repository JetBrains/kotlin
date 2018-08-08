FROM ubuntu:18.04
RUN apt-get update
RUN apt-get install -y wget
RUN apt-get install -y default-jdk
ENV version 0.8.1
ENV kotlin_native kotlin-native-linux-${version}
RUN wget https://download-cf.jetbrains.com/kotlin/native/builds/releases/${version}/linux/${kotlin_native}.tar.gz
RUN tar xzf ${kotlin_native}.tar.gz
RUN echo "export PATH=\$PATH:/${kotlin_native}/bin" > /.rc.entry
ENTRYPOINT /bin/bash --rcfile /.rc.entry