#!/usr/bin/env bash

if [ -z "$KONAN_HOME" ]; then
    PATH="$DIR/../../dist/bin:$DIR/../../bin:$PATH"
else
    PATH="$KONAN_HOME/bin:$PATH"
fi

KONAN_USER_DIR="$HOME/.konan"
KONAN_DEPS="$KONAN_USER_DIR/dependencies"
