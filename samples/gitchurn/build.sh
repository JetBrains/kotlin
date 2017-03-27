#!/usr/bin/env bash

PATH=../../dist/bin:../../bin:$PATH
DIR=.

CFLAGS_macbook=-I/opt/local/include
CFLAGS_linux=-I/usr/include
LINKER_ARGS_macbook="-L/opt/local/lib -lgit2"
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

interop -copt:$CFLAGS -def:$DIR/libgit2.def -target:$TARGET || exit 1
konanc -target $TARGET src libgit2 -nativelibrary libgit2stubs.bc -linkerArgs "$LINKER_ARGS" -o GitChurn.kexe || exit 1
