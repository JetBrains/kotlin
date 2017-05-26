#!/usr/bin/env bash


DIR=$(dirname "$0")
PATH=../../dist/bin:../../bin:$PATH

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

./buildCpp.sh

cinterop -def $DIR/src/c_interop/MessageChannel.def -copt "-I$DIR/src/cpp" -target $TARGET \
         -o $DIR/build/c_interop/MessageChannel.kt.bc || exit 1

konanc $DIR/src/kotlin-native/Concurrent.kt -library $DIR/build/c_interop/MessageChannel.kt.bc \
       -nativelibrary $DIR/build/clang/MessageChannel.bc -o $DIR/build/bin/Concurrent.kexe || exit 1

echo "Artifact path is ./build/bin/Concurrent.kexe"