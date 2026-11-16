FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive

ENV TZ=Etc/UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential autoconf automake libtool texinfo file patch curl python3 ca-certificates \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /build

COPY package.py manifests.py ./
COPY hooks/ ./hooks/

RUN mkdir /output
# Builds libffi for the container architecture (x86-64 or arm64).
ENTRYPOINT ["python3", "package.py", "--output-dir", "/output"]
