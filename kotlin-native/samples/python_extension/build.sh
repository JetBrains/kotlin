#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

if [ -z "$KONAN_HOME" ]; then
    PATH="$DIR/../../dist/bin:$DIR/../../bin:$PATH"
else
    PATH="$KONAN_HOME/bin:$PATH"
fi

KONAN_USER_DIR=${KONAN_DATA_DIR:-"$HOME/.konan"}
KONAN_DEPS="$KONAN_USER_DIR/dependencies"

# python3 shall work as well.
PYTHON=python

mkdir -p $DIR/build
cd $DIR/build

kotlinc-native -p dynamic $DIR/src/main/kotlin/Server.kt -o server

cd $DIR
sudo -S $PYTHON ${DIR}/src/main/python/setup.py install
