# We might want to switch to alpine, but it is not stable enough yet.
FROM ubuntu:20.04

ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Install crosstool-ng deps.
RUN apt-get update
RUN apt-get install -y curl gcc git g++ gperf bison flex texinfo help2man make libncurses5-dev \
    python3-dev autoconf automake libtool libtool-bin gawk wget bzip2 xz-utils unzip \
    patch libstdc++6 rsync

# Put a fix for strip.
COPY patches/github_pull_1244.patch .
# Install crosstool-ng.
RUN git clone --branch crosstool-ng-1.24.0 --depth 1 https://github.com/crosstool-ng/crosstool-ng.git && \
    cd crosstool-ng && \
    git checkout b2151f1dba2b20c310adfe7198e461ec4469172b && \
    git apply ../github_pull_1244.patch && \
    ./bootstrap && ./configure && make && make install && \
    cd .. && rm -rf crosstool-ng

# Create a user.
ARG USERNAME=ct
RUN groupadd -g 1000 $USERNAME
RUN useradd -r -u 1000 --create-home -g $USERNAME $USERNAME
USER $USERNAME
WORKDIR /home/$USERNAME

# Download zlib sources.
RUN curl -LO https://zlib.net/zlib-1.2.11.tar.gz && \
    tar -xf zlib-1.2.11.tar.gz && \
    rm zlib-1.2.11.tar.gz

# Save crosstool-ng config files.
COPY toolchains toolchains

# Used by crosstool-ng.
RUN mkdir src

ENV TARGET=x86_64-unknown-linux-gnu
ENV VERSION=gcc-8.3.0-glibc-2.19-kernel-4.9

# Add entry point.
COPY build_toolchain.sh .
ENTRYPOINT "/bin/bash" "build_toolchain.sh" ${TARGET} ${VERSION}