FROM ubuntu:20.04

ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update
RUN apt-get install -y git curl unzip ninja-build gcc g++ build-essential zlib1g-dev

# Build Python.
RUN cd /opt && \
    curl -LO https://www.python.org/ftp/python/3.6.9/Python-3.6.9.tgz && \
    tar -xf Python-3.6.9.tgz && \
    cd Python-3.6.9 && \
    ./configure && make && make install

# Get CMake.
ARG CMAKE_DIST=v3.29/cmake-3.29.7-linux-x86_64.sh
RUN cd /opt && \
    curl -Lo install_cmake.sh https://cmake.org/files/${CMAKE_DIST} && \
    chmod +x install_cmake.sh && \
    ./install_cmake.sh --skip-license --exclude-subdir --prefix=/usr

# Create a user.
ARG USERNAME=jb
RUN groupadd -g 1000 $USERNAME
RUN useradd -r -u 1000 --create-home -g $USERNAME $USERNAME
USER $USERNAME
WORKDIR /home/$USERNAME

COPY package.py .

ENTRYPOINT ["python3.6", "package.py"]
