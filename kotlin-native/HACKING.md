## Profiling the compiler

### Profiling with Async profiler

 IDEA Ultimate contains an Async sampling profiler.
 As of IDEA 2018.3 Async sampling profiler is still an experimental feature, so use Ctrl-Alt-Shift-/ on Linux,
Cmd-Alt-Shift-/ on macOS to activate it. Then start compilation in CLI with `--no-daemon` and
`-Porg.gradle.workers.max=1` flags (running Gradle task with the profiler doesn't seem to work properly) and attach
to the running process using "Run/Attach Profiler to Local Process" menu item.

 Select "K2NativeKt" or "org.jetbrains.kotlin.cli.utilities.MainKt" process.
On completion profiler will produce flame diagram which could be navigated with the mouse
(click-drag moves, wheel scales). More RAM in IDE (>4G) could be helpful when analyzing longer runs.
As Async is a sampling profiler, to get sensible coverage longer runs are important.

### Profiling with YourKit

Unlike Async profiler in IDEA, YourKit can work as an exact profiler and provide complete coverage
of all methods along with exact invocation counters.

Install the YourKit profiler for your platform from https://www.yourkit.com/java/profiler.
Set AGENT variable to the JVMTI agent provided by YourKit, like

        export AGENT=/Applications/YourKit-Java-Profiler-2018.04.app/Contents/Resources/bin/mac/libyjpagent.jnilib

To profile standard library compilation:

        ./gradlew -PstdLibJvmArgs="-agentpath:$AGENT=probe_disable=*,listen=all,tracing"  :kotlin-native:dist

To profile platform libraries start build of proper target like this:

        ./gradlew -PplatformLibsJvmArgs="-agentpath:$AGENT=probe_disable=*,listen=all,tracing"  :kotlin-native:ios_arm64PlatformLibs

To profile standalone code compilation use:

        JAVA_OPTS="-agentpath:$AGENT=probe_disable=*,listen=all,tracing" ./kotlin-native/dist/bin/konanc file.kt

Then attach to the desired application in YourKit GUI and use CPU tab to inspect CPU consuming methods.
Saving the trace may be needed for more analysis. Adjusting `-Xmx` in `$HOME/.yjp/ui.ini` could help
with the big traces.

To perform memory profiling follow the steps above, and after attachment to the running process
use "Start Object Allocation Recording" button. See https://www.yourkit.com/docs/java/help/allocations.jsp for more details.

## Compiler Gradle options

There are several gradle flags one can use for Konan build.

* **-Pbuild_flags** passes flags to the compiler used to build stdlib

        ./gradlew -Pbuild_flags="--disable lower_inline --print_ir" :kotlin-native:stdlib

* **-Pshims** compiles LLVM interface with tracing "shims". Allowing one 
    to trace the LLVM calls from the compiler.
    Make sure to rebuild the project.

        ./gradlew -Pshims=true :kotlin-native:dist

 ## Compiler environment variables

* **KONAN_DATA_DIR** changes `.konan` local data directory location (`$HOME/.konan` by default). Works both with cli compiler and gradle plugin

## Testing

### Compiler integration tests

There are a number of integration tests for the Native backend.
To run them all, use the following Gradle task:

    ./gradlew :nativeCompilerTest --continue

This takes a lot of time.
The tests are scattered across multiple Gradle projects,
and using the `--continue` flag makes sure that failures in one don't stop the others.

The Kotlin/Native compiler has many different options.
And the test infrastructure itself can run the tests with some variations.
Both can be controlled with Gradle project properties:

    ./gradlew :nativeCompilerTest --continue \
        -Pkotlin.internal.native.test.<property1Name>=<property1Value> \
        -Pkotlin.internal.native.test.<property2Name>=<property2Value>

Hint: you can save a few keystrokes by using `kn.` prefix instead of `kotlin.internal.native.test.`.

See the table of property names and values below.

| Property                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `nativeHome`            | The full path to the Kotlin/Native distribution that will be used to run tests on. If not specified, then the distribution will be built by the corresponding Gradle task as a precondition before running tests: `:kotlin-native:dist` or `:kotlin-native:${target}CrossDist`.<br/><br/>Typically, this parameter is used to run tests against a distribution that was already built and cached somewhere. For example, to reproduce a test failure on a certain Kotlin/Native build.                                                                                                              |
| `compilerClasspath`     | The full path to the Kotlin/Native compiler classpath. If not specified, then the classpath is deduced as `${nativeHome}/konan/lib/kotlin-native-compiler-embeddable.jar`<br/><br/>This property allows to override the compiler itself preserving the rest of the distribution, and this way to test various backward compatibility cases.                                                                                                                                                                                                                                                         |
| `target`                | The name of the Kotlin/Native target under test                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `mode`                  | * `ONE_STAGE_MULTI_MODULE` : Compile each test file as one or many modules (depending on MODULE directives declared in the file). Produce a KLIB per each module except the last one. Finally, produce an executable file by compiling the latest module with all other KLIBs passed as `-library` <br/>* `TWO_STAGE_MULTI_MODULE` (default): Compile each test file as one or many modules (depending on MODULE directives declared in the file). Produce a KLIB per each module. Finally, produce an executable file by passing the latest KLIB as `-Xinclude` and all other KLIBs as `-library`. |
| `forceStandalone `      | If `true` then all tests with `// KIND: REGULAR` inside test data file are executed as if they were be with `// KIND: STANDALONE`. The default is `false`.                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `compileOnly `          | If `true` then tests are fully compiled to the executable binary, but not executed afterwards. The default is `false`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `optimizationMode`      | Compiler optimization mode: `DEBUG` (default), `OPT`, `NO`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `useThreadStateChecker` | If `true` the thread state checker is enabled. The default is `false`.<br/><br/>Note: Thread state checker can be enabled only in combination with `optimizationMode=DEBUG` and `cacheMode=NO`.                                                                                                                                                                                                                                                                                                                                                                                                     |
| `gcType`                | The type of GC: `UNSPECIFIED` (default), `NOOP`, `STWMS`, `PMCS`, `CMS`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `gcScheduler`           | The type of GC scheduler: `UNSPECIFIED` (default), `ADAPTIVE`, `AGGRESSIVE`, `MANUAL`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `alloc`                 | The type of allocator: `UNSPECIFIED` (default), `STD`, `CUSTOM`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `pagedAllocator`        | Controls if allocations should be done in pages or per-object. The default is `true`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `cacheMode`             | * `NO`: no caches <br/>* `STATIC_ONLY_DIST` (default): use only caches for libs from the distribution <br/>* `STATIC_EVERYWHERE`: use caches for libs from the distribution and generate caches for all produced KLIBs<br/>* `STATIC_PER_FILE_EVERYWHERE`: same as `STATIC_EVERYWHERE`, but use per-file caches. <br/>* `STATIC_USE_HEADERS_EVERYWHERE`: same as `STATIC_EVERYWHERE`, but use header caches <br/><br/>Note: Any cache mode that permits using caches can be enabled only when thread state checker is disabled.                                                                     |
| `executionTimeout`      | Max permitted duration of each individual test execution in milliseconds                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `sanitizer`             | Run tests with sanitizer: `NONE` (default), `THREAD`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `sharedTestExecution`   | Controls how to run tests compiled into the same test executable: `false` (default) -- run executable many times, `true` -- run it once, parse the result to distinguish tests.                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `eagerGroupCreation`    | Group more tests into an executable: `false` (default), `true`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `xctest`                | Compile and run tests with XCTest: `false` (default), `true`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |

 #### Passing arbitrary binary options

It is possible to pass binary options to all tests by using `-Pkn.binaryOptions=<key=value>,<key=value>,<key=value>`.
A list of available binary options is available in [BinaryOptions.kt](../native/binary-options/src/main/kotlin/org/jetbrains/kotlin/backend/konan/BinaryOptions.kt).

 #### Generating JUnit test classes

Most compiler integration tests are defined by test data files,
while the actual JUnit test classes to run them are auto-generated based on those files.
When adding a new test data file, re-generate the test classes by running

    ./gradlew generateTests

or `Generate Tests` run configuration in IDEA.
This generates all the tests in the repository, not only the ones for Kotlin/Native.

 #### Filtering compiler integration tests with tags

Most integration tests are marked with JUnit tags.
For example, tests using K1 (the old compiler frontend) are marked with the `frontend-classic` tag.
The `-Pkotlin.native.tests.tags` Gradle project property can be used to filter tests by their tags.
The value of the property is a [JUnit tag expression](https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions).
For example, the following command will run all tests except those using K1:

    ./gradlew :nativeCompilerTest --continue -Pkotlin.native.tests.tags='!frontend-classic'

 #### Compiler integration tests in a specific Gradle project

As mentioned above, the `:nativeCompilerTest` task runs tests from many Gradle projects.
Each project is supposed to mean a specific group of compiler integration tests.
You can find the list of the projects at the definition of the `nativeCompilerTest` Gradle task
in [the root build script](../build.gradle.kts).

All those projects support the Gradle project properties controlling test execution listed above.
In addition, when running the `test` task from a specific project,
the test cases to run can be selected with the `--tests` flag:

    ./gradlew :native:native.tests:codegen-box:test --tests "org.jetbrains.kotlin.konan.test.blackbox.FirNativeCodegenBoxTestGenerated\$Box\$*"

 #### Compiler blackbox tests

The `:native:native.tests:codegen-box:test` task above runs the blackbox tests:
they check the correctness of Kotlin language features support in the Kotlin/Native compiler.
The same tests are also used in other Kotlin backends (JVM, JS) for the same purpose,
and they generally do not depend on a particular Kotlin/Native target.

A test can be ignored for certain property values with the help of test directives within
[test data files](../compiler/testData/codegen):
- `// IGNORE_NATIVE: <name>=<value>` to ignore test

Good examples are:
- `// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE`
- `// IGNORE_NATIVE: mode=ONE_STAGE_MULTI_MODULE`
- `// IGNORE_NATIVE: optimizationMode=OPT`
- `// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE && target=linux_x64`

Test will be ignored in case value of any `// IGNORE_NATIVE*` directive would match to an actual test run setting.

 ### Compiler unit tests

There are also a handful of the Kotlin/Native compiler unit tests. To run them, use the following Gradle task:

    ./gradlew :nativeCompilerUnitTest --continue

The tests are scattered across multiple projects.
See the list in the `nativeCompilerUnitTest` task definition in [the root build script](../build.gradle.kts).

 ### Runtime unit tests

To run all runtime unit tests on the host machine:

    ./gradlew :kotlin-native:runtime:hostRuntimeTests

Use `-Pgtest_filter=` to filter which tests to run (uses Google Test filter syntax).
Use `-Pgtest_timeout=` to limit how much time each test executable can take (accepts values like `30s`, `1h15m20s`, and so on).

We use [Google Test](https://github.com/google/googletest) to execute the runtime unit tests. The build automatically fetches
the specified Google Test revision to `kotlin-native/runtime/googletest`. It is possible to manually modify the downloaded GTest sources for debug
purposes; the build will not overwrite them by default.

To forcibly redownload Google Test when running tests, use the corresponding project property:

     ./gradlew :kotlin-native:runtime:hostRuntimeTests -Prefresh-gtest

or run the `downloadGoogleTest` task directly with the `--refresh` CLI key:

    ./gradlew :kotlin-native:downloadGoogleTest --refresh
    
To use a local GTest copy instead of the downloaded one, add the following line to `kotlin-native/runtime/build.gradle.kts`:

    googletest.useLocalSources("<path to local GTest sources>")

## Debugging Kotlin/Native compiler

To debug Kotlin/Native compiler with a debugger (e.g. in IntelliJ IDEA),
you should run the compiler in a special way to make it wait for a debugger connection.
There are different ways to achieve that.

### Making the compiler wait for a debugger when running Gradle build

If you use Kotlin/Native compiler as a part of your Gradle build (which is usually the case), you can simply debug
the entire Gradle process by adding `-Dorg.gradle.debug=true` to Gradle arguments.
I.e., run the build like
```shell
./gradlew $task -Dorg.gradle.debug=true
```

This will make the Gradle process wait for a debugger to connect right after start, and you will be able to connect
with IntelliJ IDEA (see below) or other debuggers.

In this case you will debug the execution of all triggered tasks.
In particular, Kotlin/Native tasks.

Note: this won't work if you have `kotlin.native.disableCompilerDaemon=true` in your Gradle properties.

### Making the command-line compiler wait for a debugger

Kotlin/Native compiler is also available in command line.
To make it wait for a debugger, you have to add certain JVM options when launching it.
These flags could be set via environment variable `JAVA_OPTS`.
The following bash script (`debug.sh`) can be used to debug:

```shell
#!/bin/bash
set -e
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005" "$@"
```

So running Kotlin/Native compiler with this script

```shell
~/debug.sh <path_to_kotlin>/kotlin-native/dist/bin/kotlinc-native ...
```

makes the compiler wait for a debugger to connect.

Additionally, even if you build with Gradle, you can extract command-line
compiler arguments from the detailed Gradle output of your project's build
process.
This will allow you to run the command-line compiler instead of Gradle, which might be helpful when debugging.
To get the detailed Gradle output, run the Gradle command with `-Pkotlin.internal.compiler.arguments.log.level=warning` flag.
See also [degrade](tools/degrade) tool -- it automates extracting Kotlin/Native command-line tools invocations from Gradle builds.

### Attaching with IntelliJ IDEA

After you instructed a Gradle process or a command-line compiler invocation to wait for a debugger connection, now it's time to connect.
It is possible to do this with different JVM debuggers.
Here is the explanation for IntelliJ IDEA.

First, set up your debug configuration in IntelliJ IDEA.
After opening Kotlin project in IDEA (more information can be found on
https://github.com/JetBrains/kotlin#build-environment-requirements and
https://github.com/JetBrains/kotlin#-working-with-the-project-in-intellij-idea), create a remote debugger config.

Use `Edit Configurations` in dropdown menu with your run/debug configurations and add a new `Remote JVM Debug`.
Ensure the port is `5005`.
Now just run this configuration. It’ll attach and let the compiler run.

## Working with C++ code in CLion

It's possible to use CLion to work on C++ code used in Kotlin/Native: 
- Kotlin/Native runtime found in `kotlin-native/runtime`
- extensions for llvm found in `kotlin-native/libllvmext` and `kotlin-native/llvmDebugInfoC`
- extensions for libclang found in `kotlin-native/libclangext`
- support code for JVM<->C++ bindings in `kotlin-native/Interop/Runtime`
- a tool to analyze minidumps used in Compiler Tests on macOS in `kotlin-native/tools/minidump-analyzer`

C++ code support is done via Compilation Database. It lets CLion detect project files and extract all the necessary compiler information, such as include paths and compilation flags.

To generate the Compilation Database, run `./gradlew :kotlin-native:compdb`.
This task generates `<path_to_kotlin>/kotlin-native/compile_commands.json` file that should be opened in CLion as a project.
Other developer tools can also use the generated Compilation Database, but may require a manual installation of `clangd` LSP.

### Kotlin/Native runtime specifics

It's possible to build Kotlin/Native runtime with dwarf debug information, which can be useful for debugging.
To do this you should add `kotlin.native.isNativeRuntimeDebugInfoEnabled=true` line to `local.properties` file.

Unfortunately, this feature might work unstable, so it's need to be additionally enabled while
compiling an application with `-Xbinary=stripDebugInfoFromNativeLibs=false` compiler flag or
the corresponding setting in the Gradle build script. After doing this, Kotlin/Native runtime in application
is debuggable in CLion, with Attach to process tool.


 ## Performance measurement

See [the performance project](performance/README.md) for details on the micro-benchmarking setup.

## LLVM

See [BUILDING_LLVM.md](BUILDING_LLVM.md) if you want to build and use your own LLVM distribution
instead of provided one.

### Using different LLVM distributions as part of Kotlin/Native compilation pipeline.

`-Xllvm-variant` compiler option allows to choose which LLVM distribution should be used during compilation.
The following values are supported:
* `user` — The compiler downloads (if necessary) and uses small LLVM distribution that contains only necessary tools. This is what compiler does by default.
* `dev` — The compiler downloads (if necessary) and uses large LLVM distribution that contains additional development tools like `llvm-nm`, `opt`, etc.
* `<absolute path>` — Use local distribution of LLVM.

### Playing with compilation pipeline.

The following compiler phases control different parts of LLVM pipeline:
1. `LinkBitcodeDependencies`. Linkage of produced bitcode with runtime and some other dependencies.
2. Running different parts of LLVM optimization pipeline:
   1. `MandatoryBitcodeLLVMPostprocessingPhase`: important postprocessing. Disabling can break generated code.
   2. `ModuleBitcodeOptimization`: Basic optimization pipeline. Something close to clang -O3
   3. `LTOBitcodeOptimization`: LTO pipeline. Slower, but better optimizations, assuming whole program knowledge.  
3. `ObjectFiles`. Compilation of bitcode with Clang.

For example, pass `-Xdisable-phases=LTOBitcodeOptimization` to skip this part of optimization pipeline for faster compilation with slower code.
Note that disabling `LinkBitcodeDependencies` or `ObjectFiles` will break compilation pipeline.

Compiler takes options for Clang from [konan.properties](konan/konan.properties) file
by combining `clangFlags.<TARGET>` and `clang<Noopt/Opt/Debug>Flags.<TARGET>` properties. 
Use `-Xoverride-konan-properties=<key_1=value_1; ...;key_n=value_n>` flag to override default values.

Please note:
1. Kotlin Native passes bitcode files to Clang instead of C or C++, so many flags won't work.
2. `-cc1 -emit-obj` should be passed because Kotlin/Native calls linker by itself.
3. Use `clang -cc1 -help` to see a list of available options.

#### Example: replace predefined LLVM pipeline with Clang options.
```shell script
CLANG_FLAGS="clangFlags.macos_x64=-cc1 -emit-obj;clangNooptFlags.macos_x64=-O2"
kotlinc-native main.kt -Xdisable-phases=MandatoryBitcodeLLVMPostprocessingPhase,ModuleBitcodeOptimization,LTOBitcodeOptimization -Xoverride-konan-properties="$CLANG_FLAGS"
```

### Dumping LLVM IR

It is possible to dump LLVM IR after a particular compiler phase.

```shell script
kotlinc-native main.kt -Xsave-llvm-ir-after=<PhaseName> -Xsave-llvm-ir-directory=<PATH>
```

`<PATH>/out.<PhaseName>.ll` will contain LLVM IR after given phase.

Passing `Codegen` phase allows to get LLVM IR right after translation from Kotlin Backend IR, and
`BitcodeOptimization` phase allows to see the result of LLVM optimization pipeline. The list of phases that support LLVM IR dumping is constantly changing, so check out compiler sources
if you want to get the full list of such phases.

## Running Clang the same way Kotlin/Native compiler does

Kotlin/Native compiler (including `cinterop` tool) has machinery that manages LLVM, Clang and native SDKs for supported targets
and runs bundled Clang with proper arguments.
To utilize this machinery, use `$dist/bin/run_konan clang $tool $target $arguments`, e.g.
```
$dist/bin/run_konan clang clang ios_arm64 1.c
```
will print and run the following command:
```
~/.konan/dependencies/clang-llvm-apple-8.0.0-darwin-macos/bin/clang \
    -B/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin \
    -fno-stack-protector -stdlib=libc++ -arch arm64 \
    -isysroot /Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS13.5.sdk \
    -miphoneos-version-min=9.0 1.c
```

The similar helper is available for LLVM tools, `$dist/bin/run_konan llvm $tool $arguments`.

### Convenient cross-compilation wrappers

For easier cross-compilation of C/C++ code, Kotlin/Native provides wrapper scripts that follow
the LLVM target triple naming convention. These scripts are auto-generated from `konan/konan.properties`
during the distribution build (via the `generateCrossCompilationWrappers` Gradle task).

The scripts are placed in `$dist/bin/cross-compile/` rather than directly in `$dist/bin/` to avoid
PATH pollution and name collisions with system-installed toolchains.

The script names use the full LLVM target triple from `konan.properties`, for example:

| Target | C Compiler | C++ Compiler |
|--------|------------|--------------|
| `linux_x64` | `x86_64-unknown-linux-gnu-clang` | `x86_64-unknown-linux-gnu-clang++` |
| `linux_arm64` | `aarch64-unknown-linux-gnu-clang` | `aarch64-unknown-linux-gnu-clang++` |
| `linux_arm32_hfp` | `arm-unknown-linux-gnueabihf-clang` | `arm-unknown-linux-gnueabihf-clang++` |
| `mingw_x64` | `x86_64-pc-windows-gnu-clang` | `x86_64-pc-windows-gnu-clang++` |
| `android_arm64` | `aarch64-unknown-linux-android-clang` | `aarch64-unknown-linux-android-clang++` |
| `android_arm32` | `arm-unknown-linux-androideabi-clang` | `arm-unknown-linux-androideabi-clang++` |
| `android_x64` | `x86_64-unknown-linux-android-clang` | `x86_64-unknown-linux-android-clang++` |
| `android_x86` | `i686-unknown-linux-android-clang` | `i686-unknown-linux-android-clang++` |
| `macos_x64` | `x86_64-apple-macos-clang` | `x86_64-apple-macos-clang++` |
| `macos_arm64` | `arm64-apple-macos-clang` | `arm64-apple-macos-clang++` |
| `ios_arm64` | `arm64-apple-ios-clang` | `arm64-apple-ios-clang++` |
| ... | ... | ... |

Scripts are generated for all targets defined in `konan.properties`, including Apple targets.
Users run only the scripts that work on their host platform.

Usage example:
```bash
# Compile a C file for Linux x64
$dist/bin/cross-compile/x86_64-unknown-linux-gnu-clang -c myfile.c -o myfile.o

# Compile a C++ file for Windows (MinGW)
$dist/bin/cross-compile/x86_64-pc-windows-gnu-clang++ -c myfile.cpp -o myfile.o

# Compile a C file for Linux ARM64
$dist/bin/cross-compile/aarch64-unknown-linux-gnu-clang -c myfile.c -o myfile.o

# Compile a C++ file for Android ARM64
$dist/bin/cross-compile/aarch64-unknown-linux-android-clang++ -c myfile.cpp -o myfile.o
```

These wrappers automatically use Kotlin/Native's sysroot and the correct compiler flags,
making it easier to build static libraries that are compatible with Kotlin/Native linking.
