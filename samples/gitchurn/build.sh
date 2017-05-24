#!/usr/bin/env bash

PATH=../../dist/bin:../../bin:$PATH
DIR=.

# Uncomment flags if your paths differ from these ones.
CFLAGS_macbook=-I/opt/local/include
#CFLAGS_macbook=-I/usr/local/include
CFLAGS_linux=-I/usr/include
LINKER_ARGS_macbook="-L/opt/local/lib -lgit2"
#LINKER_ARGS_macbook="-L/usr/local/lib -lgit2"
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

cinterop -copt $CFLAGS -def $DIR/libgit2.def -target $TARGET -o libgit2 || exit 1
konanc -target $TARGET src -library libgit2 -linkerArgs "$LINKER_ARGS" -o GitChurn || exit 1
