FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive

ENV TZ=Etc/UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc-mingw-w64-x86-64 autoconf automake libtool make texinfo file patch curl python3 ca-certificates \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /build

COPY package.py manifests.py ./
COPY hooks/ ./hooks/

RUN mkdir /output
# Cross-compiles libffi for Windows x64 using the mingw-w64 toolchain.
ENTRYPOINT ["python3", "package.py", "--target", "mingw-x64", "--output-dir", "/output"]
