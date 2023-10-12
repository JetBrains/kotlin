# Kotlin/Native  #

_Kotlin/Native_ is an LLVM backend for the Kotlin compiler, runtime
implementation, and native code generation facility using the LLVM toolchain.

 _Kotlin/Native_ is primarily designed to allow compilation for platforms where
virtual machines are not desirable or possible (such as iOS or embedded targets),
or where a developer is willing to produce a reasonably-sized self-contained program
without the need to ship an additional execution runtime.

## Using published Kotlin/Native versions

The most complete experience with Kotlin/Native can be achieved by using
[Gradle](https://kotlinlang.org/docs/native-gradle.html),
[IntelliJ IDEA](https://kotlinlang.org/docs/native-get-started.html) or
[Android Studio with KMM plugin](https://kotlinlang.org/docs/mobile/create-first-app.html)
if you target iOS.

If you are interested in using Kotlin/Native for iOS, then
[Kotlin Multiplatform Mobile portal](https://kotlinlang.org/lp/mobile/)
might be useful for you.

Command line compiler is also
[available](https://kotlinlang.org/docs/native-command-line-compiler.html).

More information can be found in the overviews of
[Kotlin/Native](https://kotlinlang.org/docs/native-overview.html)
and [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html).

On macOS Kotlin/Native requires Xcode 12.5 or newer.

## Contributing

You can contribute to Kotlin/Native in many ways.
See the relevant page [on the website](https://kotlinlang.org/docs/contribute.html).

See also the general [contribution guidelines](../docs/contributing.md) for this repository.

## Building from source

Prerequisites:
*   configure Kotlin build as [specified in main readme](../ReadMe.md#build-environment-requirements)
*   at the root directory of the repository,
    create `local.properties` file with `kotlin.native.enabled=true` line
*   macOS: Xcode 15.0 or newer
    * on `MacOS aarch64`, CInterop functionality is available only using aarch64 JDK builds, e.g.
[Eclipse Temurin 17.0.5](https://github.com/adoptium/temurin17-binaries/releases) or
[Azul Zulu JDK8](https://www.azul.com/downloads/?version=java-8-lts&os=macos&architecture=arm-64-bit&package=jdk)

      Note: using `JDK x86_64` on `MacOS aarch64` will cause `java.lang.UnsatisfiedLinkError` for `libclang.dylib`
*   Linux: glibc 2.23 or newer
*   Windows:
    * Microsoft C++ build tools for Visual Studio 2019 14.29.
      It might work with other VS2019 versions, but this was never tested.
    * Windows SDK 10.0.18362.0 or newer

The commands below should be run from either repository root or this (`kotlin-native/`) directory.
For the latter, `:kotlin-native:` task name prefix can be omitted.

To compile the basic compiler distribution from sources, run following command:

    ./gradlew :kotlin-native:dist

It will build compiler and stdlib for host target, without
[platform libraries](https://kotlinlang.org/docs/native-platform-libs.html).

To get platform libraries, add `distPlatformLibs` task, e.g.

    ./gradlew :kotlin-native:dist :kotlin-native:distPlatformLibs

To run the full build:

    ./gradlew :kotlin-native:bundle

This will produce compiler and libraries for all supported targets.
The full build can take about an hour on a Macbook Pro, but the duration can vary based on your system configuration.

After any of the commands above, `./dist` will contain Kotlin/Native distribution.
You can use it like a distribution of
[command-line compiler](https://kotlinlang.org/docs/native-command-line-compiler.html).

Or configure Gradle to use it -- just add the following line to
`gradle.properties` in your Gradle project:

    kotlin.native.home=/path/to/kotlin/kotlin-native/dist

To compile your programs with command-line compiler, use:

	./dist/bin/kotlinc-native hello.kt -o hello

For an optimized compilation, use `-opt`:

	./dist/bin/kotlinc-native hello.kt -o hello -opt

### Interoperability

To import a C or Objective-C library, use `./dist/bin/cinterop` tool.
See the [documentation](https://kotlinlang.org/docs/native-c-interop.html) for more details.

Note: on MacOS aarch64, [JDK aarch64 is required](./README.md#building-from-source)


### Running tests

For tests, use `./gradlew :native:native.tests:codegenBoxTest` and `./gradlew :kotlin-native:backend.native:tests:run`.

Note: on MacOS aarch64, for target-specific tests, [JDK aarch64 is required](./README.md#building-from-source)

For more details see [Testing](HACKING.md#Testing).

## More tips and tricks

More tips and tricks that might be useful when developing or debugging Kotlin/Native
can be found in [HACKING.md](HACKING.md)
