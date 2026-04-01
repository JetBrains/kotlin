#!/bin/bash

GRAAL_HOME=$1
shift
METADATA_FILES=("$@")

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
NI_CONFIG_DIR="$SCRIPT_DIR/current-config"
MERGED_METADATA="$SCRIPT_DIR/merged-metadata.json"
NATIVE_IMAGE_BIN=$GRAAL_HOME/bin/native-image

echo 'Starting native image build of the compiler'
echo "native-image path: $NATIVE_IMAGE_BIN"
echo "Metadata files: ${METADATA_FILES[*]}"

echo '1. Merging reachability metadata files'
"$SCRIPT_DIR/reachability/merge-metadata.sh" "$MERGED_METADATA" "${METADATA_FILES[@]}"

echo '2. Splitting merged metadata into separate config files'
"$SCRIPT_DIR/reachability/split-metadata.sh" "$MERGED_METADATA" "$NI_CONFIG_DIR"

echo '3. Building kotlin compiler embeddable'
./gradlew -q :kotlin-compiler-embeddable:embeddable

EMBEDDABLE_JAR=$(find prepare/compiler-embeddable -name 'kotlin-compiler-embeddable-2.4.255-SNAPSHOT.jar')
STDLIB_JAR=$(find libraries/ -name 'kotlin-stdlib-2.4.255-SNAPSHOT.jar')
REFLECT_JAR=$(find libraries/ -name 'kotlin-reflect-2.4.255-SNAPSHOT.jar')

CLASSPATH=$EMBEDDABLE_JAR:$STDLIB_JAR:$REFLECT_JAR
echo "Class path: $CLASSPATH"

#echo '3.5 Run java to collect reachability metadata'
#$GRAAL_HOME/bin/java \
#  --add-opens java.base/java.lang=ALL-UNNAMED \
#  --add-opens java.base/java.io=ALL-UNNAMED \
#  --add-opens java.base/java.nio=ALL-UNNAMED \
#  --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
#  --add-opens java.desktop/javax.swing=ALL-UNNAMED \
#  -agentlib:native-image-agent=config-output-dir=/Users/Azat.Abdullin/IdeaProjects/kotlin/native-image/reachability/ \
#  -cp $CLASSPATH \
#  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler


echo '4. Building native image of kotlin compiler embeddable'
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
  -o kotlinc-native \
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
