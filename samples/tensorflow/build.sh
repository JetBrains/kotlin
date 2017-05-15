#!/usr/bin/env bash

PATH=../../dist/bin:../../bin:$PATH
DIR=.
TF_TARGET_DIRECTORY="$HOME/.konan/third-party/tensorflow"
TF_TYPE="cpu" # Change to "gpu" for GPU support

CFLAGS_macbook="-I${TF_TARGET_DIRECTORY}/include"
CFLAGS_linux="-I${TF_TARGET_DIRECTORY}/include"

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
 echo "Installing TensorFlow into $TF_TARGET_DIRECTORY ..."
 mkdir -p $TF_TARGET_DIRECTORY
 curl -s -L \
   "https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow-${TF_TYPE}-${TF_TARGET}-x86_64-1.1.0.tar.gz" |
   tar -C $TF_TARGET_DIRECTORY -xz
fi

cinterop -def $DIR/tensorflow.def -copt "$CFLAGS" -target $TARGET -o tensorflow.kt.bc || exit 1
konanc $COMPILER_ARGS -target $TARGET $DIR/HelloTensorflow.kt -library tensorflow.kt.bc -o HelloTensorflow.kexe \
    -linkerArgs "-L$TF_TARGET_DIRECTORY/lib -ltensorflow" || exit 1

echo "Note: You may need to specify LD_LIBRARY_PATH or DYLD_LIBRARY_PATH env variables to $TF_TARGET_DIRECTORY/lib if the TensorFlow dynamic library cannot be found."
