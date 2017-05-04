#!/usr/bin/env bash

PATH=../../dist/bin:../../bin:$PATH
DIR=.
TF_TARGET_DIRECTORY="/opt/local"

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook; TF_TARGET=darwin ;;
  linux*)   TARGET=linux; TF_TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

var=CFLAGS_${TARGET}
CFLAGS=${!var}
var=LINKER_ARGS_${TARGET}
LINKER_ARGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

if [ ! -d $TF_TARGET_DIRECTORY/include/tensorflow ]; then
 echo "Installing TensorFlow..."
 TF_TYPE="cpu" # Change to "gpu" for GPU support
 TF_TARGET_DIRECTORY="/opt/local"
 curl -s -L \
   "https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow-${TF_TYPE}-${TF_TARGET}-x86_64-1.1.0.tar.gz" |
   sudo tar -C $TF_TARGET_DIRECTORY -xz
fi

cinterop -def $DIR/tensorflow.def -copt "$CFLAGS" -target $TARGET -o tensorflow.kt.bc || exit 1
konanc $COMPILER_ARGS -target $TARGET $DIR/HelloTensorflow.kt -library tensorflow.kt.bc -o HelloTensorflow.kexe \
    -linkerArgs "-L/opt/local/lib -ltensorflow" || exit 1
