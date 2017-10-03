#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH=$DIR/../../dist/bin:$DIR/../../bin:$PATH

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

var=CFLAGS_${TARGET}
CFLAGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

mkdir -p $DIR/build/bin
mkdir -p $DIR/build/klib

konanc $DIR/src/jsinterop/kotlin \
        -includeBinary $DIR/src/jsinterop/js/jsinterop.js \
        -p library -o $DIR/build/klib/jsinterop -target wasm32 || exit 1

konanc $DIR/src/canvas/kotlin \
        -includeBinary $DIR/src/canvas/js/canvas.js \
        -l $DIR/build/klib/jsinterop \
        -p library -o $DIR/build/klib/canvas -target wasm32 || exit 1

konanc $DIR/src/main/kotlin \
        -r $DIR/build/klib -l jsinterop -l canvas \
        -o $DIR/build/bin/html5Canvas -target wasm32 || exit 1

echo "Artifact path is $DIR/build/bin/html5Canvas.wasm"
echo "Artifact path is $DIR/build/bin/html5Canvas.wasm.js"
echo "Check out $DIR/index.html"
echo "Serve $DIR/ by an http server for the demo"
# For example run: python -m SimpleHTTPServer 8080 
