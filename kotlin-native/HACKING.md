For an overview of the code base, see [docs/native/architecture.md](../docs/native/architecture.md).

## Profiling the compiler

When building the repository, the Kotlin/Native compiler that builds the stdlib, platform libraries and caches runs inside the
Gradle daemon, in an isolated classloader. To profile those compilations, profile the Gradle daemon. For example, add the agent to
`org.gradle.jvmargs`, or attach to the daemon process.

To profile a standalone compilation, pass JVM options to the command-line compiler through `JAVA_OPTS`:

        JAVA_OPTS="-agentpath:<path to profiler agent>" ./kotlin-native/dist/bin/kotlinc-native file.kt

Any JVM profiler works:
* IntelliJ IDEA Ultimate's built-in profiler: attach to the `K2NativeKt` or `org.jetbrains.kotlin.cli.utilities.MainKt` process,
  or to the Gradle daemon.
* async-profiler.
* YourKit.

As with any sampling profiler, longer runs give better coverage.

The compiler can also report the time spent in each phase with `-Xprofile-phases`, or a summary with `-Xreport-perf`.

## Compiler Gradle options

Useful Gradle properties for building Kotlin/Native from this repository:

* `kotlin.native.enabled=true` (in `local.properties`) is required to build Kotlin/Native.
* `kotlin.native.llvm=<path>` builds against a local LLVM distribution, see [BUILDING_LLVM.md](BUILDING_LLVM.md).
* `kotlin.native.isNativeRuntimeDebugInfoEnabled=true` builds the runtime with debug info, see
  [Kotlin/Native runtime specifics](#kotlinnative-runtime-specifics).
* `-PcheckXcodeVersion=true` checks that the local Xcode major version matches `xcodeMajorVersion` from [gradle.properties](../gradle.properties).

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
| `useThreadStateChecker` | If `true` the thread state checker is enabled. The default is `false`.<br/><br/>Note: the infrastructure asserts `optimizationMode=DEBUG` when the checker is enabled; the compiler additionally requires no caches, so use `cacheMode=NO` (not asserted by the test infrastructure, IC tests are skipped instead).                                                                                                                                                                                                                                                                              |
| `gcType`                | The type of GC: `NOOP`, `STWMS`, `PMCS`, `CMS` (case-insensitive). The default is `CMS`; the infrastructure always passes an explicit `-Xbinary=gc=`, so the compiler's own default is never exercised. Any other value fails the test run.                                                                                                                                                                                                                                                                                                                                                      |
| `gcScheduler`           | The type of GC scheduler: `UNSPECIFIED` (default), `ADAPTIVE`, `AGGRESSIVE`, `MANUAL`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `alloc`                 | The legacy allocator switch (`-Xallocator=`): `UNSPECIFIED` (default), `STD`, `CUSTOM`. Scheduled for removal (KT-75914); prefer `pagedAllocator`. Specifying both `alloc` and `pagedAllocator` is an error.                                                                                                                                                                                                                                                                                                                                                                                       |
| `pagedAllocator`        | `true`/`false` → `-Xbinary=pagedAllocator=`. Unset by default, so the compiler's default (paged) applies. Cannot be combined with `alloc`.                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `cacheMode`             | * `NO`: no caches <br/>* `STATIC_ONLY_DIST`: use only caches for libs from the distribution <br/>* `STATIC_EVERYWHERE`: use caches for libs from the distribution and generate caches for all produced KLIBs<br/>* `STATIC_PER_FILE_EVERYWHERE`: same as `STATIC_EVERYWHERE`, but use per-file caches. <br/>* `STATIC_USE_HEADERS_EVERYWHERE`: same as `STATIC_EVERYWHERE`, but use header caches <br/><br/>Default: `STATIC_ONLY_DIST` when the test target is listed in `cacheableTargets.<host>` of `konan.properties` (plus `mingw_x64`), `NO` otherwise, and always `NO` under `optimizationMode=OPT`. On a non-cacheable target an explicit `STATIC_*` mode silently degrades to no caches. On `mingw_x64` only `NO` and `STATIC_ONLY_DIST` are accepted. |
| `executionTimeout`      | Max permitted duration of each individual test execution in milliseconds                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `sanitizer`             | Run tests with sanitizer: `NONE` (default), `THREAD`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `sharedTestExecution`   | Controls how to run tests compiled into the same test executable: `false` (default) -- run executable many times, `true` -- run it once, parse the result to distinguish tests.                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `eagerGroupCreation`    | Group more tests into an executable: `false` (default), `true`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `xctest`                | Compile and run tests with XCTest: `false` (default), `true`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `jdkVersion`            | The JDK major version used to run the compiler and tests (e.g., `11`, `17`, `21`). Falls back to JDK 11 when unset.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |

#### Passing arbitrary binary options

It is possible to pass binary options to all tests by using `-Pkn.binaryOptions=<key=value>,<key=value>,<key=value>`.
A list of available binary options is available in [BinaryOptions.kt](../native/binary-options/src/main/kotlin/org/jetbrains/kotlin/config/nativeBinaryOptions/BinaryOptions.kt).

#### Generating JUnit test classes

Most compiler integration tests are defined by test data files,
while the actual JUnit test classes to run them are auto-generated based on those files.
Kotlin/Native test classes are generated into `<project>/build/tests-gen/` and are not checked in.
The test task regenerates them automatically, so after adding a test data file you can just run the tests.
To refresh the generated classes for the IDE, run the `generateTests` task of the corresponding project, e.g.

    ./gradlew :native:native.tests:generateTests :native:native.tests:codegen-box:generateTests

Note that the repository-wide `./gradlew generateTests` does not generate the Kotlin/Native test classes.

#### Filtering compiler integration tests with tags

Most integration tests are marked with JUnit tags
(e.g. `codegen`, `codegen-box`, `standalone`, `gc`, `cinterop`, `objcexport`, `cexport`, `klib`, `caches`, `partial-linkage`,
`debugger`, `stdlib`).
The `-Pkotlin.native.tests.tags` Gradle project property can be used to filter tests by their tags.
The value of the property is a [JUnit tag expression](https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions).
For example, the following command will run all tests except the debugger ones:

    ./gradlew :nativeCompilerTest --continue -Pkotlin.native.tests.tags='!debugger'

`:native:native.tests` also has a task per tag, e.g. `stdlibTest`, `cinteropTest`, `gcTest`, `standaloneTest`, `klibTest`,
`cachesTest`, `partialLinkageTest`, `debuggerTest`, `infrastructureTest`
(see [its build script](../native/native.tests/build.gradle.kts)).

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
- `// IGNORE_NATIVE: <name>=<value>` marks the test as expected to fail; it still runs, and fails if it unexpectedly passes
- `// DISABLE_NATIVE: <name>=<value>` doesn't compile or run the test at all
- `// IGNORE_BACKEND: NATIVE` / `// IGNORE_BACKEND_K2: NATIVE` (from the common test infrastructure) is treated like an
  unconditional `IGNORE_NATIVE`: the test is expected to fail in every configuration, still runs, and fails if it passes

Good examples are:
- `// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE`
- `// IGNORE_NATIVE: mode=ONE_STAGE_MULTI_MODULE`
- `// IGNORE_NATIVE: optimizationMode=OPT`
- `// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE && target=linux_x64`

A test is treated as expected to fail if any of its `// IGNORE_NATIVE` directives matches the actual test run settings.

All Native test directives are defined in
[TestDirectives.kt](../native/native.tests/testFixtures/org/jetbrains/kotlin/konan/test/blackbox/support/TestDirectives.kt).

#### Test infrastructure overview

The full description of how the infrastructure works inside (settings hierarchy, grouping, the test runner and dump, the
expected-failure protocol, cache modes, class generation) is [docs/native/test-infrastructure.md](../docs/native/test-infrastructure.md).
In short, the infrastructure lives in
[native/native.tests/testFixtures](../native/native.tests/testFixtures/org/jetbrains/kotlin/konan/test/blackbox):
* `Abstract*Test.kt` are the base classes of the generated tests: `AbstractNativeBlackBoxTest` for test data–driven tests,
  `AbstractNativeSimpleTest` for hand-written ones, and specializations for cinterop, ObjC/C export, partial linkage, etc.
* `support/ConfigurationProperties.kt` maps the `kotlin.internal.native.test.*` properties above to settings.
  `support/settings/` holds those settings.
* `support/group/` turns test data into test cases (`ExtTestCaseGroupProvider` for the shared `compiler/testData`).
  It groups compatible tests into one executable, and evaluates `IGNORE_NATIVE`/`DISABLE_NATIVE`.
* `support/compilation/` invokes the compiler (`K2Native`) and cinterop in-process, loaded from the distribution under test.
  This is why the test tasks depend on `:kotlin-native:dist`.
* `support/runner/` runs the produced binaries through [executors](../native/executors) (host, simulator, emulator)
  and checks the output.

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
`LTOBitcodeOptimization` phase allows to see the result of LLVM optimization pipeline. The list of phases that support LLVM IR dumping is constantly changing, so check out compiler sources
if you want to get the full list of such phases.

It's also possible to dump LLVM IR after LLVM passes. Use `<PhaseName>:<LLVMPassName>` to dump after `LLVMPassName` while executing `PhaseName`
in Kotlin compiler. For example,

```shell script
mkdir llvm-ir
kotlinc-native main.kt -Xsave-llvm-ir-after=ModuleBitcodeOptimization:always-inline -Xsave-llvm-ir-directory=llvm-ir/
```

will dump LLVM IR in `llvm-ir/ModuleBitcodeOptimization/<...>-module-AlwaysInlinerPass-after.ll`.

Important: only use this when running the CLI compiler; the implementation sets up global variables in the shared llvm library,
which can lead to data races, if the compiler is running inside Gradle daemon.

The output logic and file naming differs from the compiler, but it matches `opt -print-after=...` exactly:
for example, if a pass is a function pass, there will be `n` files created with each of the `n` functions
on which the pass ran.

## Running Clang the same way Kotlin/Native compiler does

Kotlin/Native compiler (including `cinterop` tool) has machinery that manages LLVM, Clang and native SDKs for supported targets
and runs bundled Clang with proper arguments.
To utilize this machinery, use `$dist/bin/run_konan clang $tool $target $arguments`, e.g.
```
$dist/bin/run_konan clang clang ios_arm64 1.c
```
will print the full command line (bundled Clang from `~/.konan/dependencies/llvm-*`, the target sysroot, and the target flags
from [konan.properties](konan/konan.properties)) and run it.

The similar helper is available for LLVM tools, `$dist/bin/run_konan llvm $tool $arguments`.
