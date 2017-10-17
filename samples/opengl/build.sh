#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH=$DIR/../../dist/bin:$DIR/../../bin:$PATH

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

mkdir -p $DIR/build/bin/

konanc -target $TARGET $DIR/src/main/kotlin/OpenGlTeapot.kt \
       -o $DIR/build/bin/OpenGlTeapot || exit 1

echo "Artifact path is $DIR/build/bin/OpenGlTeapot.kexe"
