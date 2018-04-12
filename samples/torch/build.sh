#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

$DIR/downloadTorch.sh

TH_TARGET_DIRECTORY="$KONAN_USER_DIR/third-party/torch"

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook; TF_TARGET=darwin ;;
  linux*)   TARGET=linux; TF_TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

CFLAGS_macbook="-I${TH_TARGET_DIRECTORY}/include"
CFLAGS_linux="-I${TH_TARGET_DIRECTORY}/include"

var=CFLAGS_${TARGET}
CFLAGS=${!var}
var=LINKER_ARGS_${TARGET}
LINKER_ARGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

mkdir -p $DIR/build/c_interop/
mkdir -p $DIR/build/bin/

cinterop -def $DIR/src/main/c_interop/torch.def -compilerOpts "$CFLAGS" -labels $TARGET \
     -copt -I$TH_TARGET_DIRECTORY/include/TH -o $DIR/build/c_interop/torch || exit 1

SOURCE_DIR=$DIR/src/main/kotlin

konanc $COMPILER_ARGS -target $TARGET $SOURCE_DIR/ClassifierDemo.kt $SOURCE_DIR/Disposable.kt \
       $SOURCE_DIR/Tensors.kt $SOURCE_DIR/Modules.kt $SOURCE_DIR/Dataset.kt $SOURCE_DIR/SmallDemos.kt \
       -library $DIR/build/c_interop/torch \
       -o $DIR/build/bin/HelloTorch \
       -linkerOpts "-L$TH_TARGET_DIRECTORY/lib -lATen" || exit 1

echo "Note: You may need to specify LD_LIBRARY_PATH or DYLD_LIBRARY_PATH env variables to $TH_TARGET_DIRECTORY/lib if the ATen dynamic library cannot be found."

echo "Artifact path is $DIR/build/bin/HelloTorch.kexe"
