# Kotlin Compiler Embeddable Native Image distribution

A GraalVM-compiled native image of `kotlin-compiler-embeddable` that runs as a
standalone executable. Behaves like the regular `kotlinc` but with some limitations &mdash;
mainly, no support for plugins. Produces a native image distribution which is similar
in its structure to the standard `kotlinc` distribution.

## Distribution layout

The distribution is produced in [prepare/compiler-native-image/build/dist/kotlinc-native-image](build/dist/kotlinc-native-image) directory and
has the following structure:

* `bin/kotlinc-native-image[.exe]` &mdash; native binary;
* `bin/kotlinc-native-image.[sh|bat]` &mdash; launcher wrappers;
* `lib/` &mdash; runtime classpath jars;
* `license/` &mdash; license information.

## Building

Produce a native image binary:
```
./gradlew :kotlin-compiler-native-image:kotlincNativeImage
```

The task requires a GraalVM JDK 25 toolchain, which is automatically resolved via Gradle.

Produce a native image distribution:
```
./gradlew :kotlin-compiler-native-image:kotlincNativeImageDist
```


## Running

Similar to JVM `kotlinc`, the native image binary requires some configuration. To simplify
the user experience, the distribution also provides `kotlinc-native-image.[sh|bat]` wrapper scripts
that do all the necessary configuration of the binary. Namely, they configure:

* `-Djava.home` &mdash; resolved to the `JAVA_HOME` environment variable;
* `-Dkotlin.home` &mdash; resolved to the location of the distribution root.

These wrapper scripts may be used as a drop-in replacement for the JVM `kotlinc`:

```bash
dist/kotlinc-native-image/bin/kotlinc-native-image.sh path/to/Foo.kt -d out/
```

## Tests

Run the test suite with:
```
./gradlew :kotlin-compiler-native-image:nativeImageBoxTest
```

The test suite mirrors the compiler's JVM box-codegen tests: it is
generated from the same test data under [compiler/testData/codegen/box](../../compiler/testData/codegen/box)
and behaves similarly to the JVM tests. The goal is to ensure that the behavior of the native image
compiler is the same as its JVM counterpart.

The current setup is quite ad-hoc:

* tests are excluded from the default `compilerTest` aggregate and run only via
  the dedicated `nativeImageBoxTest` task; the main goal is to not intervene with
  pre-existing workflows while still ensuring that the native image works correctly;
* test setup manually performs the source code preprocessing and directives handling;
* `JVM_IR` is treated as the target backend, so a test is skipped if it contains 
  `// IGNORE_BACKEND: JVM_IR` directive (or its equivalent);
* multifile tests are not supported;
* tests that use helper functions (`helpers.*`) are skipped.


## Reachability metadata

GraalVM reachability metadata is stored in [prepare/compiler-native-image/resources/META-INF/native-image](resources/META-INF/native-image).

Initial metadata was collected by running the Kotlin Compiler Embeddable JAR with the [GraalVM Tracing Agent](https://www.graalvm.org/latest/reference-manual/native-image/metadata/AutomaticMetadataCollection/)
on the same set of compiler box tests [compiler/testData/codegen/box](../../compiler/testData/codegen/box).

Metadata can be updated manually when necessary. You can use this script template:

```bash
#!/bin/bash

GRAAL_HOME=$JAVA_HOME
UPDATE_REACHABILITY_METADATA=false
NATIVE_IMAGE_BIN=$GRAAL_HOME/bin/native-image

echo '--- Building kotlin compiler dist ---'
./gradlew -q :dist

echo '--- Building kotlin compiler embeddable ---'
./gradlew -q :kotlin-compiler-embeddable:embeddable

EMBEDDABLE_JAR=$(find prepare/compiler-embeddable -name 'kotlin-compiler-embeddable-*.jar')
STDLIB_JAR='dist/kotlinc/lib/kotlin-stdlib.jar'
REFLECT_JAR='dist/kotlinc/lib/kotlin-reflect.jar'
COROUTINES_JAR='dist/kotlinc/lib/kotlin-coroutines-core-jvm.jar'
ANNOTATIONS_JAR=$(find dist/kotlinc/lib/ -name 'annotations-*.jar')

CLASSPATH=$EMBEDDABLE_JAR:$STDLIB_JAR:$REFLECT_JAR:$COROUTINES_JAR:$ANNOTATIONS_JAR

$GRAAL_HOME/bin/java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.io=ALL-UNNAMED \
  --add-opens java.base/java.nio=ALL-UNNAMED \
  --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens java.desktop/javax.swing=ALL-UNNAMED \
  -cp $CLASSPATH \
  -Dkotlin.home=dist/kotlinc \
  -Djava.home=$GRAAL_HOME \
  -agentlib:native-image-agent=config-merge-dir=prepare/compiler-native-image/resources/META-INF/native-image/org/jetbrains/kotlin/kotlin-compiler-embeddable \
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  $@
```
