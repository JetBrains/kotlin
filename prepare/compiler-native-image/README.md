# Kotlin Compiler Embeddable Native Image distribution

A GraalVM-compiled native image of `kotlin-compiler-embeddable` that runs as a
standalone executable. Behaves like the regular `kotlinc` but with some limitations &mdash;
mainly, a limited list of supported compiler plugins (see [Plugins](#Plugins)). Produces a native image distribution which is
similar in its structure to the standard `kotlinc` distribution.

## Distribution layout

The distribution is produced in [prepare/compiler-native-image/build/dist](build/dist) directory and
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

The task requires a GraalVM JDK version 25.1.3+ toolchain, which is automatically resolved via Gradle.

Produce a native image distribution:
```
./gradlew :kotlin-compiler-native-image:kotlincNativeImageDist
```

You can also produce native image distribution release artifacts in
[prepare/compiler-native-image/build/artifacts](build/artifacts) via:
```
./gradlew :kotlin-compiler-native-image:kotlincNativeImageArtifacts
```

Additional build properties that require Oracle GraalVM:
* `-Pkotlin.build.native-image.use-g1=true` enables the G1 garbage collector for the compiler native image. It provides significant performance benefits.
* `-Pkotlin.build.native-image.dynamic-plugins=true` enables dynamic plugin loading support, see [Plugins](#plugins) for more details.
* `-Pkotlin.build.native-image.use-pgo=true` enable profile-guided optimizations using [kotlin-compiler-embeddable.iprof](kotlin-compiler-embeddable.iprof),
see [Profile-Guided Optimizations](#profile-guided-optimizations) for more details.

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

For a quick sanity check, use:
```
./gradlew :kotlin-compiler-native-image:nativeImageSmokeTest
```
It compiles a "Hello World" with the native image compiler and verifies that the compilation succeeds.

For the full suite, run:
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

Metadata can be regenerated when necessary. For a quick update, use:
```
./gradlew :kotlin-compiler-native-image:generateReachabilityMetadataSmoke
```
It will run the reachability metadata collection on the sample "Hello World" program.

For a full regeneration over all the box test data, use (takes ~3&ndash;4 hours):
```
./gradlew :kotlin-compiler-native-image:generateReachabilityMetadataBox
```

## IDE

IDE run configurations are available both for running native image tests and 
collecting reachability metadata. You can find them in the "Compiler Native Image" run configuration folder.

## Plugins

Kotlin Compiler Native Image by default only supports a limited list of built-in compiler plugins:
* allopen
* assignment
* Compose
* lombok
* noarg
* power-assert
* sam-with-receiver
* kotlinx-serialization

Attempting to use other plugins will result in a runtime error on the compiler startup.

It is possible, however, to enable (experimental) support for dynamic compiler plugin loading
thanks to GraalVM's [Project Crema](https://github.com/oracle/graal/issues/11327). To enable it,
set `kotlin.build.native-image.dynamic-plugins` Gradle property to `true` in CLI when building the executable
or in `gradle.properties` file:
```bash
./gradlew -Pkotlin.build.native-image.dynamic-plugins=true :kotlin-compiler-native-image:kotlincNativeImageDist
```

### Technical limitations of the dynamic plugin loading

* According to our experiments, dynamically loaded compiler plugins suffer 5--15% performance overhead. At the same time, native image with
dynamic plugin loading still preforms better than JVM compiler
* To enable runtime class loading, we preserve additional metadata about the classes that are accessed dynamically. This results in the significant increase
of the native image size.
* All preserved packages are currently listed in the [preserved-packages.txt](preserved-packages.txt) file. This list was compiled manually by
dynamically loading currently bundled compiler plugins. This list may not be complete, meaning that attmpting to load other compiler plugins
may result in a runtime error. In that case, you may need to add the missing packages to the file and rebuild the native image.

## Profile-Guided Optimizations

GraalVM Native Image supports [Profile-Guided Optimizations](https://www.graalvm.org/latest/reference-manual/native-image/optimizations-and-performance/PGO/).
PGO provides native image a way to compete with JVM's JIT compiler: a way to optimize the performance and size of the native image based
on the execution profile of the application. However, while JIT can do that at the runtime using the actual runtime behaviour, native image
requires the profile to be collected ahead of time.

Currently, we provide a [kotlin-compiler-embeddable.iprof](kotlin-compiler-embeddable.iprof) profile collected locally on the build of 
[kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) library. While **this profile is not fully production-ready**, it already
provides some [performance benefits](https://youtrack.jetbrains.com/issue/KT-90902/Native-Image-enable-g1-garbage-collector#focus=Comments-27-14594504.0-0).
So it is still more beneficial than no profile at all. 

We are planning to collect and provide a more comprehensive profile for the Kotlin compiler in the future.

### Collecting a custom profile

To collect a custom profile you need to rebuild the compiler native image with `--pgo-instrument` option. You may use `-Pkotlin.build.native-image.pgo-instrument=true`
option to do so:
```bash
./gradlew -Pkotlin.build.native-image.pgo-instrument=true :kotlin-compiler-native-image:kotlincNativeImageArtifacts
```

After that, you can use the newly built image to collect a profile on a given workload. You can also specify where to
store the collected profile using `-XX:ProfilesDumpFile=file.iprof` option, be default it will produce a `default.iprof` file.
```bash
./prepare/compiler-native-image/build/dist/bin/kotlinc-native-image.sh -XX:ProfilesDumpFile=prepare/compiler-native-image/kotlin-compiler-embeddable.iprof HelloWorld.kt
```

You can also collect multiple profiles and merge them into a single file using `native-image-utils`, see [GraalVM documentation](https://www.graalvm.org/latest/reference-manual/native-image/optimizations-and-performance/PGO/merging-profiles/)
for more details.
