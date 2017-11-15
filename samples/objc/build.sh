#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

mkdir -p $DIR/build/bin/

konanc -target macbook $DIR/src/main/kotlin/Window.kt \
       -o $DIR/build/bin/window || exit 1

echo "Artifact path is $DIR/build/bin/window.kexe"
