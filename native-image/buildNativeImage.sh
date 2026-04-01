#!/bin/bash

GRAAL_HOME=$1
REACHABILITY_METADATA=$2

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
NI_CONFIG_DIR="$SCRIPT_DIR/current-config"
NATIVE_IMAGE_BIN=$GRAAL_HOME/bin/native-image

echo 'Starting native image build of the compiler'
echo "native-image path: $NATIVE_IMAGE_BIN"

echo '0. Splitting reachability metadata into separate config files'
"$SCRIPT_DIR/splitMetadata.sh" "$REACHABILITY_METADATA" "$NI_CONFIG_DIR"

echo '1. Building kotlin compiler embeddable'
./gradlew -q :kotlin-compiler-embeddable:embeddable

EMBEDDABLE_JAR=$(find prepare/compiler-embeddable -name 'kotlin-compiler-embeddable-2.4.255-SNAPSHOT.jar')
STDLIB_JAR=$(find libraries/ -name 'kotlin-stdlib-2.4.255-SNAPSHOT.jar')
REFLECT_JAR=$(find libraries/ -name 'kotlin-reflect-2.4.255-SNAPSHOT.jar')

CLASSPATH=$EMBEDDABLE_JAR:$STDLIB_JAR:$REFLECT_JAR
echo "Class path: $CLASSPATH"

echo '2. Building native image of kotlin compiler embeddable'
$NATIVE_IMAGE_BIN \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.io=ALL-UNNAMED \
  --add-opens java.base/java.nio=ALL-UNNAMED \
  --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens java.desktop/javax.swing=ALL-UNNAMED \
  -H:+AddAllCharsets \
  -H:+UnlockExperimentalVMOptions \
  -H:+AllowJRTFileSystem \
  -H:ConfigurationFileDirectories=$NI_CONFIG_DIR \
  -cp $CLASSPATH \
  -o kotlincn \
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
