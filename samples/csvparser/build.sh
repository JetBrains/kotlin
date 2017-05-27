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

cinterop -def $DIR/src/c_interop/stdio.def -copt "$CFLAGS" -target $TARGET -o $DIR/build/c_interop/stdio.kt.bc || exit 1

konanc $COMPILER_ARGS -target $TARGET $DIR/src/kotlin-native/CsvParser.kt -library $DIR/build/c_interop/stdio.kt.bc \
       -o $DIR/build/bin/CsvParser.kexe || exit 1

echo "Artifact path is ./build/bin/CsvParser.kexe"
