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

## Building from source

Prerequisites:
*   configure Kotlin build as [specified in main readme](../ReadMe.md#build-environment-requirements)
*   at the root directory of the repository,
    create `local.properties` file with `kotlin.native.enabled=true` line
*   on macOS install Xcode 12.5 or newer
*   on Fedora 26+ ```yum install ncurses-compat-libs``` may be needed
*   on recent Ubuntu ```apt install libncurses5``` is needed

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
The full build can take about an hour on a Macbook Pro.

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

### Running tests

For tests, use:

	./gradlew :kotlin-native:backend.native:tests:run
