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

 ### Compiler blackbox tests

There are blackbox tests that check the correctness of Kotlin language features support in the Kotlin/Native compiler.
The same tests are also used in other Kotlin backends (JVM, JS) for the same purpose, and they generally do not depend on a particular Kotlin/Native target.

To run blackbox compiler tests use:

    ./gradlew :native:native.tests:codegenBoxTest

* **--tests** allows one to choose test suite(s) or test case(s) to run.

      ./gradlew :native:native.tests:codegenBoxTest --tests "org.jetbrains.kotlin.konan.blackboxtest.NativeCodegenBoxTestGenerated\$Box\$*"

* There are also Gradle project properties that can be used to control various aspects of blackbox tests. Example:

      ./gradlew :native:native.tests:codegenBoxTest \
          -Pkotlin.internal.native.test.<property1Name>=<property1Value> \
          -Pkotlin.internal.native.test.<property2Name>=<property2Value>

| Property                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `nativeHome`            | The full path to the Kotlin/Native distribution that will be used to run tests on. If not specified, then the distribution will be built by the corresponding Gradle task as a precondition before running tests: `:kotlin-native:dist` or `:kotlin-native:${target}CrossDist`.<br/><br/>Typically, this parameter is used to run tests against a distribution that was already built and cached somewhere. For example, to reproduce a test failure on a certain Kotlin/Native build.                                                                                                              |
| `compilerClasspath`     | The full path to the Kotlin/Native compiler classpath. If not specified, then the classpath is deduced as `${nativeHome}/konan/lib/kotlin-native-compiler-embeddable.jar`<br/><br/>This property allows to override the compiler itself preserving the rest of the distribution, and this way to test various backward compatibility cases.                                                                                                                                                                                                                                                         |
| `target`                | The name of the Kotlin/Native target under test                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `mode`                  | * `ONE_STAGE_MULTI_MODULE` : Compile each test file as one or many modules (depending on MODULE directives declared in the file). Produce a KLIB per each module except the last one. Finally, produce an executable file by compiling the latest module with all other KLIBs passed as `-library` <br/>* `TWO_STAGE_MULTI_MODULE` (default): Compile each test file as one or many modules (depending on MODULE directives declared in the file). Produce a KLIB per each module. Finally, produce an executable file by passing the latest KLIB as `-Xinclude` and all other KLIBs as `-library`. |
| `forceStandalone `      | If `true` then all tests with `// KIND: REGULAR` inside test data file are executed as if they were be with `// KIND: STANDALONE`. The default is `false`.                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `compileOnly `          | If `true` then tests are fully compiled to the executable binary, but not executed afterwards. The default is `false`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `optimizationMode`      | Compiler optimization mode: `DEBUG` (default), `OPT`, `NO`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `memoryModel`           | The memory model: `LEGACY` or `EXPERIMENTAL` (default)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `useThreadStateChecker` | If `true` the thread state checker is enabled. The default is `false`.<br/><br/>Note: Thread state checker can be enabled only in combination with `optimizationMode=DEBUG`, `memoryModel=EXPERIMENTAL` and `cacheMode=NO`.                                                                                                                                                                                                                                                                                                                                                                         |
| `gcType`                | The type of GC: `UNSPECIFIED` (default), `NOOP`, `STMS`, `CMS`<br/><br/>Note: The GC type can be specified only in combination with `memoryModel=EXPERIMENTAL`.                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `gcScheduler`           | The type of GC scheduler: `UNSPECIFIED` (default), `DISABLED`, `WITH_TIMER`, `ON_SAFE_POINTS`, `AGGRESSIVE`<br/><br/>Note: The GC scheduler type can be specified only in combination with `memoryModel=EXPERIMENTAL`.                                                                                                                                                                                                                                                                                                                                                                              |
| `cacheMode`             | * `NO`: no caches <br/>* `STATIC_ONLY_DIST` (default): use only caches for libs from the distribution <br/>* `STATIC_EVERYWHERE`: use caches for libs from the distribution and generate caches for all produced KLIBs<br/><br/>Note: Any cache mode that permits using caches can be enabled only when thread state checker is disabled.                                                                                                                                                                                                                                                           |
| `executionTimeout`      | Max permitted duration of each individual test execution in milliseconds                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `sanitizer`             | Run tests with sanitizer: `NONE` (default), `THREAD`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |

### Target-specific tests

There are also tests that are very Native-backend specific: tests for Kotlin/Native-specific function, C-interop tests, linkage tests, etc.
In common, they are called "target-specific tests".

To run Kotlin/Native target-specific tests use (takes time):

    ./gradlew :kotlin-native:backend.native:tests:sanity  2>&1 | tee log                             # quick one
    ./gradlew :kotlin-native:backend.native:tests:run  2>&1 | tee log                                # sanity tests + platform tests

* **-Pprefix** allows one to choose tests by prefix to run.

        ./gradlew -Pprefix=array :kotlin-native:backend.native:tests:run

* **-Ptest_flags** passes flags to the compiler used to compile tests

        ./gradlew -Ptest_flags="--time" :kotlin-native:backend.native:tests:array0

* **-Ptest_target** specifies cross target for a test run.

        ./gradlew -Ptest_target=raspberrypi :kotlin-native:backend.native:tests:array0

* **-Premote=user@host** sets remote test execution login/hostname. Good for cross compiled tests.

        ./gradlew -Premote=kotlin@111.22.33.444 :kotlin-native:backend.native:tests:run

* **-Ptest_verbose** enables printing compiler args and other helpful information during a test execution.

        ./gradlew -Ptest_verbose :kotlin-native:backend.native:tests:mpp_optional_expectation

* **-Ptest_two_stage** enables two-stage compilation of tests. If two-stage compilation is enabled, test sources are compiled into a klibrary
  and then a final native binary is produced from this klibrary using the -Xinclude compiler flag.

        ./gradlew -Ptest_two_stage :kotlin-native:backend.native:tests:array0

* **-Ptest_with_cache_kind=static|dynamic** enables using caches during testing.

* **-Ptest_compile_only** allows one to only compile tests, without actually running them. It is useful for testing compilation pipeline in
  case of targets that are tricky to execute tests on.

 ### Runtime unit tests
 
To run runtime unit tests on the host machine for both mimalloc and the standard allocator:

    ./gradlew :kotlin-native:runtime:hostRuntimeTests
       
To run tests for only one of these two allocators, run `:kotlin-native:runtime:hostStdAllocRuntimeTests` or `:kotlin-native:runtime:hostMimallocRuntimeTests`.

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
To get the detailed Gradle output, run the Gradle command with `-i` flag.
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

## Developing Kotlin/Native runtime in CLion

It's possible to use CLion to develop C++ runtime code efficiently and to have navigation in C++ code.
To open runtime code in CLion as project and use all provided features of CLion, use a compilation database.
It lets CLion detect project files and extract all the necessary compiler information, such as include paths and compilation flags.
To generate a compilation database for the Kotlin/Native runtime, run the Gradle task
`./gradlew :kotlin-native:compdb`.
This task generates `<path_to_kotlin>/kotlin-native/compile_commands.json` file that should be opened in CLion as project.
Other developer tools also can use generated compilation database, but then `clangd` tool should be installed manually.

Also, it's possible to build Kotlin/Native runtime with dwarf debug information, which can be useful for debugging.
To do this you should add `kotlin.native.isNativeRuntimeDebugInfoEnabled=true` line to `local.properties` file. Note, that changing
this property requires clean compiler rebuild with gradle daemon restart.

Unfortunately, this feature works quite unstable because of using several llvm versions simultaneously,
so it's need to be additionally enabled while compiling application with `-Xbinary=stripDebugInfoFromNativeLibs=false`
compiler flag or corresponding setting in gradle build script. After doing this, Kotlin/Native runtime in application
is debuggable in CLion, with Attach to process tool.


 ## Performance measurement
 ### Pre-requisite
  **konanRun** task needs built compiler and platform POSIX libs. To test against working tree make sure to run

    ./gradlew :kotlin-native:dist :kotlin-native:distPlatformLibs

 ### Run tests
 To measure performance of Kotlin/Native compiler on existing benchmarks:
 
    cd kotlin-native/performance
    ../../gradlew :konanRun
 
 **konanRun** task can be run separately for one/several benchmark applications:
 
    cd kotlin-native/performance
    ../../gradlew :cinterop:konanRun
    
 **konanRun** task has parameter `filter` which allows to run only some subset of benchmarks:
 
    cd kotlin-native/performance
    ../../gradlew :cinterop:konanRun --filter=struct,macros
    ../../gradlew :ring:konanRun --filter=Euler.problem9,ForLoops.charArrayIndicesLoop
    
 Or you can use `filterRegex` if you want to specify the filter as regexes:
 
    cd kotlin-native/performance
    ../../gradlew :ring:konanRun --filterRegex=String.*,Loop.*
    
 There us also verbose mode to follow progress of running benchmarks
 
    cd kotlin-native/performance
    ../../gradlew :cinterop:konanRun --verbose
    
    > Task :performance:cinterop:konanRun
    [DEBUG] Warm up iterations for benchmark macros
    [DEBUG] Running benchmark macros
    ...
    
 There are also tasks for running benchmarks on JVM (pay attention, some benchmarks e.g. cinterop benchmarks can't be run on JVM)
 
    cd kotlin-native/performance
    ../../gradlew :jvmRun

 You can use the `compilerArgs` property to pass flags to the compiler used to compile the benchmarks:

    cd kotlin-native/performance
    ../../gradlew :konanRun -PcompilerArgs="--time -g"

 ### Analyze the results
 Files with results of benchmarks run are saved in `kotlin-native/performance/build` folder: `nativeReport.json` for konanRun and `jvmReport.json` for jvmRun.
 You can change the output filename by setting the `nativeJson` property for konanRun and `jvmJson` for jvmRun:

    cd kotlin-native/performance
    ../../gradlew :ring:konanRun --filter=String.*,Loop.* -PnativeJson=stringsAndLoops.json

 To compare different results use benchmarksAnalyzer tool:

    ./gradlew macos_arm64PlatformLibs  # use target of your laptop here instead
    cd kotlin-native/tools/benchmarksAnalyzer
    ../../../gradlew build
    ./build/bin/<target>/benchmarksAnalyzerReleaseExecutable/benchmarksAnalyzer.kexe <file1> <file2>
    
 Tool has several renders which allow produce output report in different forms (text, html, etc.). To set up render use flag `--render/-r`.
 Output can be redirected to file with flag `--output/-o`.
 To get detailed information about supported options, please use `--help/-h`.
 
 Analyzer tool can compare both local files and files placed on Artifactory/TeamCity.
 
 File description stored on Artifactory
 
    artifactory:<build number>:<target (Linux|Windows10|MacOSX)>:<filename>
    
 Example
    
    artifactory:1.2-dev-7942:Windows10:nativeReport.json
    
 File description stored on TeamCity
  
     teamcity:<build locator>:<filename>
     
 Example
     
     teamcity:id:42491947:nativeReport.json
     
 Pay attention, user and password information(with flag `-u <username>:<password>`) should be provided to get data from TeamCity.

 By default analyzing tool splits benchmarks into stable and unstable taking information from database. If you have no connection to inner network please use `-f` flag.

    ./benchmarksAnalyzer.kexe -f <file1> <file2>

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
2. `BitcodeOptimization`. Running LLVM optimization pipeline.
3. `ObjectFiles`. Compilation of bitcode with Clang.

For example, pass `-Xdisable-phases=BitcodeOptimization` to skip optimization pipeline.
Note that disabling `LinkBitcodeDependencies` or `ObjectFiles` will break compilation pipeline.

Compiler takes options for Clang from [konan.properties](konan/konan.properties) file
by combining `clangFlags.<TARGET>` and `clang<Noopt/Opt/Debug>Flags.<TARGET>` properties. 
Use `-Xoverride-konan-properties=<key_1=value_1; ...;key_n=value_n>` flag to override default values.

Please note:
1. Kotlin Native passes bitcode files to Clang instead of C or C++, so many flags won't work.
2. `-cc1 -emit-obj` should be passed because Kotlin/Native calls linker by itself.
3. Use `clang -cc1 -help` to see a list of available options.
 
Another useful compiler option is `-Xtemporary-files-dir=<PATH>` which allows
 specifying a directory for intermediate compiler artifacts like bitcode and object files.
For example, it allows to store LLVM IR after a particular compiler phase.

```shell script
konanc main.kt -Xsave-llvm-ir-after=BitcodeOptimization -Xtemporary-files-dir=<PATH>
```

`<PATH>/out.BitcodeOptimization.ll` will contain LLVM IR after LLVM optimization pipeline.

#### Example: replace predefined LLVM pipeline with Clang options.
```shell script
CLANG_FLAGS="clangFlags.macos_x64=-cc1 -emit-obj;clangNooptFlags.macos_x64=-O2"
konanc main.kt -Xdisable-phases=BitcodeOptimization -Xoverride-konan-properties="$CLANG_FLAGS"
```

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
