#!/bin/bash

#
# Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

# Script for building reproducible-maven.zip from sources. This is a full set of artifacts published to maven central during
# the Kotlin release process.

# Run the script in the root Kotlin directory.

set -e

if [ $# -lt 3 ]; then
    echo "Not enough arguments provided. Usage: $0 DEPLOY_VERSION BUILD_NUMBER KOTLIN_NATIVE_VERSION"
    exit 1
fi

DEPLOY_VERSION=$1
BUILD_NUMBER=$2
KOTLIN_NATIVE_VERSION=$3

echo "DEPLOY_VERSION=$DEPLOY_VERSION"
echo "BUILD_NUMBER=$BUILD_NUMBER"
echo "KOTLIN_NATIVE_VERSION=$KOTLIN_NATIVE_VERSION"

# Update versions in pom.xml
mvn -DnewVersion=$DEPLOY_VERSION -DgenerateBackupPoms=false -DprocessAllModules=true -f libraries/pom.xml versions:set

# Build part of kotlin and publish it to the local maven repository and to build/repo directory
./gradlew \
  -PdeployVersion=$DEPLOY_VERSION \
  -Pbuild.number=$BUILD_NUMBER \
  -Pversions.kotlin-native=$KOTLIN_NATIVE_VERSION \
  -Pteamcity=true \
  --no-daemon \
  --info \
  publish publishToMavenLocal

# Build maven part and publish it to the same build/repo
mvn \
  -f libraries/pom.xml \
  clean deploy \
  -Ddeploy-url=file://$(pwd)/build/repo \
  -DskipTests

# Prepare for reproducibility check
mkdir -p build/repo-reproducible
cp -R build/repo/. build/repo-reproducible
# maven-metadata contains lastUpdated section with the build time
find build/repo-reproducible -name "maven-metadata.xml*" -exec rm -rf {} \;
# spdx SBOM contains creationInfo with datetime
find build/repo-reproducible -name "*.spdx.json" -exec rm -rf {} \;
# Each file has own timestamp that would affect zip file hash if not aligned
find build/repo-reproducible -exec touch -t "198001010000" {} \;
cd build/repo-reproducible && find . -type f | sort | zip -X reproducible-maven-$DEPLOY_VERSION.zip -@ && cd -
