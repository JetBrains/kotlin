#!/bin/bash

cp gradle/verification-metadata.xml gradle/verification-metadata.xml.bak

if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i '' -e '/<components>/,/<\/components>/d' gradle/verification-metadata.xml
else
  sed -i -e '/<components>/,/<\/components>/d' gradle/verification-metadata.xml
fi

./gradlew \
  --write-verification-metadata sha256 \
  -Pkotlin.native.enabled=true \
  -Pbootstrap.local=true \
  -Pbootstrap.local.path=build/repo \
  --continue \
  resolveDependencies

mv gradle/verification-metadata.xml gradle/verification-metadata-for-bootstrap.xml
mv gradle/verification-metadata.xml.bak gradle/verification-metadata.xml
