#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

PYTHON=python

konanc -p dynamic src/main/kotlin/Server.kt -o server
sudo $PYTHON src/main/python/setup.py install
