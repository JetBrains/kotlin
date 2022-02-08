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

To run blackbox compiler tests from JVM Kotlin use (takes time):

    ./gradlew :kotlin-native:run_external

* **-Pfilter** allows one to choose test files to run.

        ./gradlew -Pfilter=overflowLong.kt :kotlin-native:run_external

* **-Pprefix** allows one to choose external test directories to run. Only tests from directories with given prefix will be executed.

        ./gradlew -Pprefix=build_external_compiler_codegen_box_cast :kotlin-native:run_external

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

    ./gradlew :kotlin-native:hostRuntimeTests
       
To run tests for only one of these two allocators, run `:kotlin-native:hostStdAllocRuntimeTests` or `:kotlin-native:hostMimallocRuntimeTests`.

We use [Google Test](https://github.com/google/googletest) to execute the runtime unit tests. The build automatically fetches
the specified Google Test revision to `kotlin-native/runtime/googletest`. It is possible to manually modify the downloaded GTest sources for debug
purposes; the build will not overwrite them by default.

To forcibly redownload Google Test when running tests, use the corresponding project property:

     ./gradlew :kotlin-native:hostRuntimeTests -Prefresh-gtest

or run the `downloadGTest` task directly with the `--refresh` CLI key:

    ./gradlew :kotlin-native:downloadGTest --refresh
    
To use a local GTest copy instead of the downloaded one, add the following line to `kotlin-native/runtime/build.gradle.kts`:

    googletest.useLocalSources("<path to local GTest sources>")

## Debugging Kotlin/Native compiler

In order to debug Kotlin/Native compiler using IntelliJ IDEA, you should run it from the command line in a special way to wait for the debbuger connection. This requires adding certain JVM options before running the compiler. These flags could be set via environment variable `JAVA_OPTS`.
The following bash script (`debug.sh`) can be used to debug:

```shell
#!/bin/bash
set -e
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005" "$@"
```

Running Kotlin/Native compiler with this script

```shell
~/debug.sh <path_to_kotlin>/kotlin-native/dist/bin/konanc ...
```

makes the compiler wait for the remote debugger to connect.

You can get ther command line output of the compiler from the detailed Gradle output of your project's build process.  To see the the detailed Gradle output, run the gradle command with `-i` flag.

The next step is setting up your debug configuration in IntelliJ IDEA. After opening Kotlin project in IDEA (more information can be found on
https://github.com/JetBrains/kotlin#build-environment-requirements and
https://github.com/JetBrains/kotlin#-working-with-the-project-in-intellij-idea), create a remote debugger config.

Use `Edit Configurations` in dropdown menu with your run/debug configurations and add a new `Remote JVM Debug`.
In settings, select the same port you specified in `JAVA_OPTS` in the `debug.sh` script.
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
  
 Firstly, it's necessary to build analyzer tool to have opportunity to compare different performance results:
 
    cd kotlin-native/tools/benchmarksAnalyzer
    ../../../gradlew build
    
 To measure performance of Kotlin/Native compiler on existing benchmarks:
 
    cd kotlin-native/performance
    ../../gradlew :konanRun

 **NOTE**: **konanRun** task needs built compiler and libs. To test against working tree make sure to run

    ./gradlew :kotlin-native:dist :kotlin-native:distPlatformLibs

 before **konanRun**
    
 **konanRun** task can be run separately for one/several benchmark applications:
 
    cd kotlin-native/performance
    ../../gradlew :cinterop:konanRun
    
 **konanRun** task has parameter `filter` which allows to run only some subset of benchmarks:
 
    cd kotlin-native/performance
    ../../gradlew :cinterop:konanRun --filter=struct,macros
    
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
    
 Files with results of benchmarks run are saved in `performance/build/nativeReport.json` for konanRun and `jvmReport.json` for jvmRun.
 You can change the output filename by setting the `nativeJson` property for konanRun and `jvmJson` for jvmRun:

    cd kotlin-native/performance
    ../../gradlew :ring:konanRun --filter=String.*,Loop.* -PnativeJson=stringsAndLoops.json

 You can use the `compilerArgs` property to pass flags to the compiler used to compile the benchmarks:

    cd kotlin-native/performance
    ../../gradlew :konanRun -PcompilerArgs="--time -g"

 To compare different results run benchmarksAnalyzer tool:
 
    cd kotlin-native/tools/benchmarksAnalyzer/build/bin/<target>/benchmarksAnalyzerReleaseExecutable/
    ./benchmarksAnalyzer.kexe <file1> <file2>
    
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

### Testing native

For a quick check use:
```
$ ./gradlew :kotlin-native:sanity 2>&1 | tee log
```

For a longer, more thorough testing build the complete build. Make sure you are running it on a macOS. 


Have a complete build:

```
$ ./gradlew :kotlin-native:bundle # includes dist as its part
```

then run two test sets:

```
$ ./gradlew :kotlin-native:backend.native:tests:run 2>&1 | tee log

$ ./gradlew :kotlin-native:backend.native:tests:runExternal -Ptest_two_stage=true 2>&1 | tee log

```

## LLVM

See [BUILDING_LLVM.md](BUILDING_LLVM.md) if you want to build and use your own LLVM distribution
instead of provided one.

### Using different LLVM distributions as part of Kotlin/Native compilation pipeline.

`llvmHome.<HOST_NAME>` variable in `<distribution_location>/konan/konan.properties` controls 
which LLVM distribution Kotlin/Native will use in its compilation pipeline. 
You can replace its value with either `$llvm.<HOST_NAME>.{dev, user}` to use one of predefined distributions
or pass an absolute to your own distribution.
Don't forget to set `llvmVersion.<HOST_NAME>` to the version of your LLVM distribution.

#### Example. Using LLVM from an absolute path.
Assuming LLVM distribution is installed at `/usr` path, one can specify a path to it 
with the `-Xoverride-konan-properties` option:
```
konanc main.kt -Xoverride-konan-properties=llvmHome.linux_x64=/usr
```

### Playing with compilation pipeline.

Following compiler phases control different parts of LLVM pipeline:
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
to specify a directory for intermediate compiler artifacts like bitcode and object files.

#### Example 1. Bitcode right after IR to Bitcode translation.
```shell script
konanc main.kt -produce bitcode -o bitcode.bc
```

#### Example 2. Bitcode after LLVM optimizations.
```shell script
konanc main.kt -Xtemporary-files-dir=<PATH> -o <OUTPUT_NAME>
```
`<PATH>/<OUTPUT_NAME>.kt.bc` will contain bitcode after LLVM optimization pipeline.

#### Example 3. Replace predefined LLVM pipeline with Clang options.
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
