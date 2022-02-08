FROM ubuntu:20.04

ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && apt-get upgrade -y
RUN apt-get install -y build-essential git gcc pkg-config glib-2.0 libglib2.0-dev libsdl1.2-dev libaio-dev libcap-dev libattr1-dev libpixman-1-dev

RUN git clone --branch v5.1.0 --depth 1 git://git.qemu.org/qemu.git && \
    cd qemu && \
    git checkout d0ed6a69d399ae193959225cdeaa9382746c91cc && \
    git submodule update --init --recursive

WORKDIR /qemu

COPY build.sh .

ENV OUTPUT_DIR="/output"

ENTRYPOINT "/bin/bash" "build.sh" ${OUTPUT_DIR}