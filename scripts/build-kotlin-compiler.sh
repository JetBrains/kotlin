#!/bin/bash

#
# Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

# Script for building kotlin-compiler.zip from sources.
# Run the script in the root Kotlin directory.

set -e

if [ $# -lt 2 ]; then
    echo "Not enough arguments provided. Usage: $0 DEPLOY_VERSION BUILD_NUMBER"
    exit 1
fi

DEPLOY_VERSION=$1
BUILD_NUMBER=$2

echo "DEPLOY_VERSION=$DEPLOY_VERSION"
echo "BUILD_NUMBER=$BUILD_NUMBER"

# Build dist/kotlin-compiler.zip
./gradlew --info -PdeployVersion=$DEPLOY_VERSION -Pbuild.number=$BUILD_NUMBER -Pteamcity=true zipCompiler --no-daemon