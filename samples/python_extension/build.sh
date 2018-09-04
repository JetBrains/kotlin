#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

PYTHON=python

konanc -p dynamic ${DIR}/src/main/kotlin/Server.kt -o server
sudo $PYTHON ${DIR}/src/main/python/setup.py install
