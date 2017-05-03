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

cinterop -def $DIR/tensorflow.def -copt "$CFLAGS" -target $TARGET -o tensorflow.kt.bc || exit 1
konanc $COMPILER_ARGS -target $TARGET $DIR/HelloTensorflow.kt -library tensorflow.kt.bc -o HelloTensorflow.kexe \
    -linkerArgs "-L/usr/local/lib -ltensorflow" || exit 1
