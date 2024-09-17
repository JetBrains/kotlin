#!/bin/bash

#
# Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

if [ $# -ne 1 ]; then
    echo "Usage plugins/compose/import-compose-runtime-snapshot.sh <version>"
    exit 1
fi

snapshotId="$1"
snapshotRuntimeRepo="https://androidx.dev/snapshots/builds/$snapshotId/artifacts/repository/androidx/compose/runtime/"

echo "Requesting maven-metadata.xml from $snapshotRuntimeRepo"
snapshotMetadata=$(curl --request GET -L --url "$snapshotRuntimeRepo/runtime/maven-metadata.xml")
snapshotVersion=$(echo "$snapshotMetadata" | sed -nE "s/^.*<version>(.*)<\/version>.*$/\1/p" )

runtimeMetadataUrl="$snapshotRuntimeRepo/runtime-jvmstubs/$snapshotVersion/maven-metadata.xml"
echo "Requesting artifact metadata from $snapshotRuntimeRepo"
artifactMetadata=$(curl --request GET -L --url "$runtimeMetadataUrl")
artifactVersion=$(echo "$artifactMetadata" | sed -nE "s/^.*<value>(.*)<\/value>.*/\1/p" | head -n 1)

currentDir=$(dirname "$0")
versionFile="$currentDir/compose-runtime-snapshot-versions.toml"
echo "Writing $artifactVersion and $snapshotId to $versionFile"
sed -i '' -E "s/snapshot-id = \"[^\"]+\"/snapshot-id = \"$snapshotId\"/" "$versionFile"
sed -i '' -E "s/snapshot-version = \"[^\"]+\"/snapshot-version = \"$artifactVersion\"/" "$versionFile"