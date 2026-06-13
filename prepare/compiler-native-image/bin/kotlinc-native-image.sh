#!/usr/bin/env bash

# Based on scalac from the Scala distribution
# Copyright 2002-2011, LAMP/EPFL
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

KOTLINC_BINARY_NAME=kotlinc-native-image

# Based on findScalaHome() from scalac script
findKotlinHome() {
    local source="${BASH_SOURCE[0]}"
    while [ -h "$source" ] ; do
        local linked="$(readlink "$source")"
        local dir="$(cd -P $(dirname "$source") && cd -P $(dirname "$linked") && pwd)"
        source="$dir/$(basename "$linked")"
    done
    (cd -P "$(dirname "$source")/.." && pwd)
}

KOTLINC_HOME_DIR="$(findKotlinHome)"
KOTLINC_BINARY_DIR="${KOTLINC_HOME_DIR}/bin"

if [ -z "$JAVA_HOME" ]; then
  echo "error: JAVA_HOME is not set; ${KOTLINC_BINARY_NAME} requires JAVA_HOME environment variable" >&2
  exit 1
fi

exec "${KOTLINC_BINARY_DIR}/${KOTLINC_BINARY_NAME}" \
  -Djava.home="${JAVA_HOME}" \
  -Dkotlin.home="${KOTLINC_HOME_DIR}" \
  "$@"
