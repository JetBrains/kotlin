#!/bin/bash

#
# Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

if [ $# -ne 1 ]; then
    echo "Usage plugins/compose/import-compose-to-androidx.sh path/to/aosp/root"
    echo "NOTE: Script should be started from the repository root!"
    exit 1
fi

androidxPath="$1/frameworks/support"

if ! test -d "$androidxPath"; then
  echo "Could not find AndroidX repo at $androidxPath"
  exit 1
fi

composeSnapshotRepoPath="$androidxPath/compose/compiler/compose-compiler-snapshot-repository/"
androidxVersionsPath="$androidxPath/gradle/libs.versions.toml"

./gradlew -Dmaven.repo.local="$composeSnapshotRepoPath" install

# Substitute compose compiler snapshot version in libs.versions.toml
sed -i '' -E 's/composeCompilerPlugin = "[^"]+"/composeCompilerPlugin = "2.3.255-SNAPSHOT"/g' "$androidxVersionsPath"