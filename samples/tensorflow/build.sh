#!/usr/bin/env bash

./downloadTensorflow.sh

DIR=$(dirname "$0")
PATH=../../dist/bin:../../bin:$PATH

TF_TARGET_DIRECTORY="$HOME/.konan/third-party/tensorflow"
TF_TYPE="cpu" # Change to "gpu" for GPU support

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook; TF_TARGET=darwin ;;
  linux*)   TARGET=linux; TF_TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

CFLAGS_macbook="-I${TF_TARGET_DIRECTORY}/include"
CFLAGS_linux="-I${TF_TARGET_DIRECTORY}/include"

var=CFLAGS_${TARGET}
CFLAGS=${!var}
var=LINKER_ARGS_${TARGET}
LINKER_ARGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

rm -rf $DIR/build/
mkdir $DIR/build/
mkdir $DIR/build/c_interop/
mkdir $DIR/build/bin/

cinterop -def $DIR/src/c_interop/tensorflow.def -copt "$CFLAGS" -target $TARGET \
         -o $DIR/build/c_interop/tensorflow.kt.bc || exit 1

konanc $COMPILER_ARGS -target $TARGET $DIR/src/kotlin-native/HelloTensorflow.kt \
       -library $DIR/build/c_interop/tensorflow.kt.bc \
       -o $DIR/build/bin/HelloTensorflow.kexe \
       -linkerArgs "-L$TF_TARGET_DIRECTORY/lib -ltensorflow" || exit 1

echo "Note: You may need to specify LD_LIBRARY_PATH or DYLD_LIBRARY_PATH env variables to $TF_TARGET_DIRECTORY/lib if the TensorFlow dynamic library cannot be found."

echo "Artifact path is ./build/bin/HelloTensorflow.kexe"
