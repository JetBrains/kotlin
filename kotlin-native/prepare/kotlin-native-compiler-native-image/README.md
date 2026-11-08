# Kotlin/Native compiler native image

An **opt-in** GraalVM native image of the Kotlin/Native compiler (`kotlin-native-compiler-embeddable.jar`).

The JVM compiler pays for JVM startup and JIT warm-up on every invocation, which dominates the short compilations
of an edit-compile loop (debug binaries linked with incremental caches). The native image starts instantly.
On a 120-file synthetic project on linux_x64, a one-file edit cycle (`-p library` of the changed module +
`-p program -g` linked with `-Xic-cache-dir`) went from ~3.9 s with the JVM launcher to ~1.0 s with the image.

## Building

```
./gradlew :kotlin-native:prepare:kotlin-native-compiler-native-image:konancNativeImage
```

The task produces `build/bin/konanc-native-image`. It requires a GraalVM toolchain of the version enforced by
`resolveNativeImageExecutable` (resolved by Gradle; pass `-Porg.gradle.java.installations.paths=<GraalVM home>`
to use a specific installation) and ~14 GB of memory.

## Using

The image is never used implicitly. Opt in by pointing `KONAN_NATIVE_IMAGE` to it when running the compiler
from a Kotlin/Native distribution:

```
KONAN_NATIVE_IMAGE=/path/to/konanc-native-image kotlin-native/dist/bin/kotlinc-native hello.kt -o hello
```

Only the compiler (`konanc`) is run from the image, other tools (`cinterop`, `klib`, ...) keep using the JVM.
JVM options (`-J...`, `JAVA_OPTS`) do not apply to the image; system properties (`-D...`) are passed through.
The opt-in is supported by the Unix launcher (`run_konan`) only.

## Testing

```
./gradlew :kotlin-native:prepare:kotlin-native-compiler-native-image:nativeImageSmokeTest
```

The test builds the image and the distribution, then compiles and incrementally re-links a program through the
`kotlinc-native` launcher with the opt-in.

## Reachability metadata

The image can only use reflection, resources, JNI and so on that are declared in the reachability metadata
in [resources/META-INF/native-image](resources/META-INF/native-image):

* `kotlin-native-compiler-traced` is collected by the GraalVM tracing agent and can be regenerated;
* `kotlin-native-compiler-handwritten` covers what tracing captures only partially (e.g. all CLI arguments).

A code path that was not exercised during tracing fails at runtime with `MissingReflectionRegistrationError`
or a missing resource. To extend the traced metadata, run the JVM compiler from the distribution with the agent
on the workload in question (it merges into the existing file), then rebuild the image:

```
<GraalVM>/bin/java \
    -agentlib:native-image-agent=config-merge-dir=kotlin-native/prepare/kotlin-native-compiler-native-image/resources/META-INF/native-image/org.jetbrains.kotlin/kotlin-native-compiler-traced \
    -ea -Xmx3G -Dfile.encoding=UTF-8 -Dkonan.home=kotlin-native/dist --enable-native-access=ALL-UNNAMED \
    -cp kotlin-native/dist/konan/lib/kotlin-native-compiler-embeddable.jar \
    org.jetbrains.kotlin.cli.utilities.MainKt konanc <compiler arguments>
```

The current traced metadata was collected on linux_x64 by compiling KLIBs and linking debug executables with
incremental caches (clean, no-op and incremental builds).

## Limitations

* Metadata coverage: workloads beyond the ones above (cinterop, `-opt`, test runners, static and dynamic
  libraries, Apple frameworks, other hosts) have not been traced and may fail.
* No JIT: the image is faster than a cold JVM, but slower than a JVM that has warmed up on many compilations.
