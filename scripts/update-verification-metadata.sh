#!/bin/bash

if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i '' -e '/<components>/,/<\/components>/d' gradle/verification-metadata.xml
else
  sed -i -e '/<components>/,/<\/components>/d' gradle/verification-metadata.xml
fi

./gradlew \
  --write-verification-metadata sha256 \
  -Pkotlin.native.enabled=true \
  resolveDependencies

./gradlew publish -Pkotlin.native.enabled=false

./gradlew \
  --write-verification-metadata sha256 \
  -Pkotlin.native.enabled=true \
  -Pbootstrap.local=true \
  -Pbootstrap.local.path=build/repo \
  resolveDependencies

