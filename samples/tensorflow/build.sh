#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

$DIR/downloadTensorflow.sh

# KONAN_USER_DIR is set by konan.sh
TF_TARGET_DIRECTORY="$KONAN_USER_DIR/third-party/tensorflow"
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

mkdir -p $DIR/build/c_interop/
mkdir -p $DIR/build/bin/

cinterop -def $DIR/src/main/c_interop/tensorflow.def -compilerOpts "$CFLAGS" -target $TARGET \
	 -o $DIR/build/c_interop/tensorflow || exit 1

konanc $COMPILER_ARGS -target $TARGET $DIR/src/main/kotlin/HelloTensorflow.kt \
       -library $DIR/build/c_interop/tensorflow \
       -o $DIR/build/bin/HelloTensorflow \
       -linkerOpts "-L$TF_TARGET_DIRECTORY/lib -ltensorflow" || exit 1

echo "Note: You may need to specify LD_LIBRARY_PATH or DYLD_LIBRARY_PATH env variables to $TF_TARGET_DIRECTORY/lib if the TensorFlow dynamic library cannot be found."

echo "Artifact path is $DIR/build/bin/HelloTensorflow.kexe"
