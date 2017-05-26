#!/usr/bin/env bash

DIR=$(dirname "$0")
PATH=../../dist/bin:../../bin:$PATH

CFLAGS_macbook="-I/opt/local/include -copt -I/usr/local/include"
CFLAGS_linux=-I/usr/include
LINKER_ARGS_macbook="-L/usr/local/lib -L/opt/local/lib -lgit2"
LINKER_ARGS_linux="-L/usr/lib/x86_64-linux-gnu -lgit2"

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

rm -rf $DIR/build/
mkdir $DIR/build/
mkdir $DIR/build/c_interop/
mkdir $DIR/build/bin/

cinterop -copt $CFLAGS -def $DIR/src/c_interop/libgit2.def -target $TARGET \
         -o $DIR/build/c_interop/libgit2.kt.bc || exit 1

konanc -target $TARGET $DIR/src/kotlin-native -library $DIR/build/c_interop/libgit2.kt.bc -linkerArgs "$LINKER_ARGS" \
       -o $DIR/build/bin/GitChurn.kexe || exit 1

echo "Artifact path is ./build/bin/GitChurn.kexe"
