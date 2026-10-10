#!/usr/bin/env bash

# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

KOTLINC_WEB_IMAGE_NAME=kotlinc-wasm.js

# Based on findScalaHome() from scalac script
findKotlinHome() {
    local source="${BASH_SOURCE[0]}"
    while [ -h "$source" ] ; do
        local linked="$(readlink "$source")"
        local dir="$(cd -P "$(dirname "$source")" && cd -P "$(dirname "$linked")" && pwd)"
        source="$dir/$(basename "$linked")"
    done
    (cd -P "$(dirname "$source")/.." && pwd)
}

KOTLINC_HOME_DIR="$(findKotlinHome)"
KOTLINC_BINARY_DIR="${KOTLINC_HOME_DIR}/bin"

NODE_EXECUTABLE="${KOTLIN_WEB_IMAGE_NODE:-node}"

if ! command -v "${NODE_EXECUTABLE}" > /dev/null 2>&1; then
  echo "error: '${NODE_EXECUTABLE}' is not found; the web image requires Node.js to run" >&2
  echo "       set the KOTLIN_WEB_IMAGE_NODE environment variable to point to a Node.js executable" >&2
  exit 1
fi

# '--experimental-wasm-exnref' enables the WebAssembly exception handling proposal,
# which is required by the GraalVM Web Image runtime on Node.js versions prior to 25.
exec "${NODE_EXECUTABLE}" \
  --experimental-wasm-exnref \
  "${KOTLINC_BINARY_DIR}/${KOTLINC_WEB_IMAGE_NAME}" \
  "$@"
