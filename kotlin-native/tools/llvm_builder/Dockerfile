FROM ubuntu:16.04

ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update
RUN apt-get install -y git cmake curl unzip ninja-build gcc g++ build-essential zlib1g-dev

# Build Python.
RUN cd /opt && \
    curl -LO https://www.python.org/ftp/python/3.6.9/Python-3.6.9.tgz && \
    tar -xf Python-3.6.9.tgz && \
    cd Python-3.6.9 && \
    ./configure && make && make install

# Create a user.
ARG USERNAME=jb
RUN groupadd -g 1000 $USERNAME
RUN useradd -r -u 1000 --create-home -g $USERNAME $USERNAME
USER $USERNAME
WORKDIR /home/$USERNAME

COPY package.py .

ENTRYPOINT ["python3.6", "package.py"]
