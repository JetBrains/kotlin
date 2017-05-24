#!/usr/bin/env bash

PATH=../../dist/bin:../../bin:$PATH
DIR=.

CFLAGS_macbook=-I/opt/local/include
CFLAGS_linux=-I/usr/include/x86_64-linux-gnu
LINKER_ARGS_macbook="-L/opt/local/lib -lcurl"
LINKER_ARGS_linux="-L/usr/lib/x86_64-linux-gnu -lcurl"

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

var=CFLAGS_${TARGET}
CFLAGS=${!var}
var=LINKER_ARGS_${TARGET}
LINKER_ARGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

cinterop -copt "$CFLAGS" -copt -I. -copt -I/usr/include -def $DIR/libcurl.def -target $TARGET -o libcurl || exit 1
konanc -target $TARGET src -library libcurl -linkerArgs "$LINKER_ARGS" -o Curl || exit 1
