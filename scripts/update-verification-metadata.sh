#!/bin/bash

if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i '' -e '/<components>/,/<\/components>/d' gradle/verification-metadata.xml
else
  sed -i -e '' 'r/<components>/,/<\/components>/d' gradle/verification-metadata.xml
fi
./gradlew -i --write-verification-metadata sha256,md5 -Pkotlin.native.enabled=true resolveDependencies
