#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

SUFFIX=kexe
if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  msys*)    TARGET=mingw; SUFFIX=exe ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

var=CFLAGS_${TARGET}
CFLAGS=${!var}
var=LINKER_ARGS_${TARGET}
LINKER_ARGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

mkdir -p $DIR/build/bin/

konanc $COMPILER_ARGS -target $TARGET $DIR/src/main/kotlin/Workers.kt \
       -o $DIR/build/bin/Workers || exit 1

echo "Artifact could be found at $DIR/build/bin/Workers.$SUFFIX"
