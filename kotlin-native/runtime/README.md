# Kotlin/Native runtime and stdlib

This folder contains both the Kotlin/Native runtime and Kotlin/Native stdlib.

## stdlib

The common code lives in [libraries/stdlib](../../libraries/stdlib). Native-specific sources are:
- [src/main/kotlin](src/main/kotlin)
- [../Interop/Runtime](../Interop/Runtime) for `kotlinx.cinterop` package
- and some parts are written in C++ and considered part of the [runtime](#Runtime)

`:kotlin-native:runtime:stdlibBuildTask` builds the standard library. `:native:native.tests:stdlibTest` will run the standard library tests.

## Runtime

The sources should follow [the coding convention](codestyle/cpp/README.md).

For IDE support, use `:kotlin-native:compdb` to generate the Compilation Database in `kotlin-native/compile_commands.json` which is supported
by CLion and clangd.

`:kotlin-native:runtime:hostRuntime` builds the runtime for the current host. `:kotlin-native:runtime:<target>Runtime` can be used
to build for the specific target, or `:kotlin-native:runtime:assemble` to build for all supported targets.

`:kotlin-native:runtime:hostRuntimeTests` runs the tests for the current host (the tests will also be rerun with all supported sanitizers).
Use `-Pgtest_filter=` to filter which tests to run (uses Google Test filter syntax).
Use `-Pgtest_timeout=` to limit how much time each test executable can take (accepts values like `30s`, `1h15m20s`, and so on).

To help with code formatting, [.clang-format](.clang-format) file is placed in this folder.
`:kotlin-native:libllvmext:clangFormat` task can be used to run
`git-clang-format -f $(git merge-base origin/master HEAD) -- kotlin-native/libllvmext/`, which will
format only the changed files. The task accepts optional `--parent=<branch>` (to specify a branch
other than `origin/master`) and `--interactive` (which adds `-p` flag to `git-clang-format` to
interactively accept or reject formatting patches).

### Project structure

The runtime is split into multiple modules: the main [runtime](src/main) and a number of optional modules (e.g. [objc](src/objc) for ObjC
interop support or [cms GC](src/gc/cms) with Concurrent Mark&Sweep GC implementation).

The main module contains:
* C++ parts of the [Native stdlib](#stdlib)
  * [dtoa](src/main/cpp/dtoa): vendored part of Apache Harmony for float <-> string
  * scattered around the code
* Core runtime code
  * [mm](src/main/cpp/mm): mostly memory management, the name comes from legacy MM / new MM split which doesn't make much sense anymore
  * lots of files in [the root dir](src/main/cpp)
* API declarations for other modules to implement; see [Modules](#modules) for details
* Utilities
  * [std_support](src/main/cpp/std_support): additions to C++ stdlib (mostly ported from future standards or from proposals)
  * [objc_support](src/main/cpp/objc_support): C++ wrappers for ObjC API
  * [concurrent](src/main/cpp/concurrent): various concurrency primitives like spin locks or thread-state-aware locks
  * lots of files in [the root dir](src/main/cpp)

#### Modules

* Allocator and finalizer
  * [Common code and API](src/main/cpp/alloc)
  * [Default](src/alloc/custom)
  * [Legacy](src/alloc/legacy)
* Calls Checker: sanitizer-like instrumentation to help catch external function calls in the wrong thread state
  (usually indicates a bug in the runtime or the code generator)
  * [API](src/main/cpp/CallsChecker.hpp)
  * [No-op](src/externalCallsChecker/noop)
  * [Implementation](src/externalCallsChecker/impl): runtime for the Calls Checker instrumentation; the instrumentation lives in [libllvmext](../libllvmext)
* [Compiler interface](src/compiler_interface): API for the compiler (compiled into a bitcode file, that's parsed by the compiler)
* Crash handler
  * [API](src/main/cpp/CrashHandler.hpp)
  * [No-op](src/crashHandler/noop): does nothing
  * [Implementation](src/crashHandler/impl) with [Breakpad](src/breakpad) (with additional sources fetched from [its repo](https://github.com/google/breakpad)):
    only supported on macOS, generates minidump on crash, can be analyzed with [minidump-analyzer](../tools/minidump-analyzer)
* [Debug](src/debug): API for the [lldb python plugin](../llvmDebugInfoC/src/scripts/konan_lldb.py)
* [Exceptions support](src/exceptions_support): used for dynamic compiler caches (untested)
* GC
  * [Common code and API](src/main/cpp/gc)
  * [CMS](src/gc/cms): Concurrent Mark&Sweep
  * [No-op](src/gc/noop): does nothing
  * [PMCS](src/gc/pmcs): Parallel Mark & Concurrent Sweep
  * [STMS](src/gc/stms): Stop-the-world Mark&Sweep
* GC Scheduler (also known as GC Pacer): decides when to trigger the GC
  * [Common code and API](src/main/cpp/gcScheduler)
  * [Adaptive](src/gcScheduler/adaptive): heuristics based on heap usage
  * [Aggressive](src/gcScheduler/aggressive): triggers GC on every newly encountered safepoint (used as a form of GC stress testing)
  * [Manual](src/gcScheduler/manual): GC is only triggered when explicitly requested
* [Launcher](src/launcher): defines an entry point, when Kotlin/Native is compiled into an executable
* ObjC interop and Swift Export support
  * [Main module parts](src/main/cpp) (files starting with `ObjC*` and `swiftExportRuntime` folder)
  * [objc](src/objc): ObjC class definitions that might get renamed in the final binary
  * [objCExport](src/objcExport): `KotlinBase` definition that might also get renamed in the final binary; but also used by Swift Export
* Source Info: symbolication support for stack traces
  * [API](src/main/cpp/SourceInfo.h) additionally backed by `compiler::getSourceInfo` in [CompilerConstants.hpp](src/main/cpp/CompilerConstants.hpp)
  * [Core Symbolication-based](src/source_info/core_symbolication): symbolication based on Core Symbolication; only for Apple platforms
  * [libbacktrace-based](src/source_info/libbacktrace) with [libbacktrace](src/libbacktrace): symbolication based on libbacktrace;
    supported on Apple platforms and on Linux
* [utfcpp](src/utfcpp): vendored library for UTF-8 support
* [XCTest launcher](src/xctest_launcher): defines an entry point, when Kotlin/Native is compiled into test bundle; only for Apple platforms
* Runtime unit tests support:
  * [test_support](src/test_support): defines symbols usually defined by the code generator and a GTest test launcher
  * `googletest` and `googlemock` are fetched from [their repo](https://github.com/google/googletest)