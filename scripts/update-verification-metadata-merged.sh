#!/bin/bash

set -e

META=gradle/verification-metadata.xml
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

strip_components() {
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' -e '/<components>/,/<\/components>/d' "$META"
  else
    sed -i -e '/<components>/,/<\/components>/d' "$META"
  fi
}

strip_components
./gradlew \
  --write-verification-metadata sha256 \
  -Pkotlin.native.enabled=true \
  resolveDependencies
cp "$META" "$TMP/normal.xml"

strip_components
./gradlew \
  --write-verification-metadata sha256 \
  -Pkotlin.native.enabled=true \
  -Pbootstrap.local=true \
  -Pbootstrap.local.path=build/repo \
  --continue \
  resolveDependencies
cp "$META" "$TMP/bootstrap.xml"

# Merge: union of both passes; on a shared coordinate the normal entry wins.
kotlinc -script scripts/merge-verification-metadata.kts \
  "$TMP/normal.xml" "$TMP/bootstrap.xml" "$META"
