#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

if [ -z "$KONAN_HOME" ]; then
    PATH="$DIR/../../dist/bin:$DIR/../../bin:$PATH"
else
    PATH="$KONAN_HOME/bin:$PATH"
fi

mkdir -p $DIR/build/bin/

konanc -target iphone $DIR/src/main/kotlin/main.kt \
       -o $DIR/build/bin/uikit || exit 1

echo "Artifact path is $DIR/build/bin/uikit.kexe"
