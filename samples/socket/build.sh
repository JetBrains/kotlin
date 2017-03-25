#!/usr/bin/env bash

PATH=../../dist/bin:../../bin:$PATH
DIR=.

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

interop -def:$DIR/sockets.def -copt:"$CFLAGS" -target:$TARGET || exit 1
konanc $COMPILER_ARGS -target $TARGET $DIR/EchoServer.kt sockets -nativelibrary socketsstubs.bc -o EchoServer.kexe || exit 1
