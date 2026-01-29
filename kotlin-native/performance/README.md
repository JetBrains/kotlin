# Micro Benchmarks for Kotlin/Native

To run the benchmarks you need a built Kotlin/Native distribution with the platform libraries:
* either specify the full path to the existing distribution with `-Pkotlin.native.home=<path>`
* or run `./gradlew -Pkotlin.native.enabled=true :kotlin-native:dist :kotlin-native:distPlatformLibs`
  in the root of the `git` repository (that is in the `kotlin` folder not in the current `kotlin/kotlin-native/performance`)
  to build the distribution from sources

To run all the benchmarks just run `./gradlew` from this folder. The output will be placed in `build/nativeReport.json`.

## Analysis

To pretty-print the report, or compare different reports, [benchmarksAnalyzer](benchmarksAnalyzer) can be used.

Run `./gradlew :buildAnalyzer` and the analyzer executable will be placed in `build/benchmarksAnalyzer.[k]exe`.

Passing a single file (e.g. `build/benchmarksAnalyzer.kexe <report.json>`) pretty-prints the report.

Passing 2 files (e.g. `build/benchmarksAnalyzer.kexe <report.json> <baseline-report>.json`) will compare
2 reports.

## Configuration options

Pass as Gradle Properties (i.e. as `-P<option>=<value>`):
* `kotlin.native.home` - path to the native distribution
* `nativeWarmup`, `attempts` - configure the warmup and the iteration count of each benchmark
* `buildType` - whether to build in `debug` or `release` (default) mode
* `filter`, `filterRegex` - comma-separated list of benchmarks to run

The full list can be found [here](buildSrc/src/main/kotlin/Properties.kt).

## Benchmark groups

The benchmarks are split into several groups:
* `ring` - regular benchmarks
* `startup` - benchmarks for startup performance and for once-initialized stuff (e.g. Kotlin `object {}`)
* `numerical` - compares performance of computational code between pure Kotlin and cinterop-called C code
* `cinterop` - benchmarks using C import
* `objcinterop` - benchmarks using ObjC import
* `swiftinterop` - benchmarks written in Swift calling Kotlin using ObjC export
* `helloworld` - compilation time benchmark (compiles hello world with a CLI compiler)

`./gradlew :konanRun` (or just `./gradlew`) runs all the groups; use something like `./gradlew :ring :startup`
to only run a subset of groups.

## Writing benchmarks

Excepting `swiftinterop` (see [the corresponding](swiftinterop/README.md) documentation),
all the benchmarks are written using `kotlinx-benchmark`.

### kotlinx-benchmark

See [the official documentation](https://github.com/Kotlin/kotlinx-benchmark/blob/master/docs/writing-benchmarks.md)
on how to write benchmarks using `kotlinx-benchmark`.

**NOTE**: there's an ongoing migration to `kotlinx-benchmark` from the previous custom solution; some of the existing
benchmarks therefore don't follow best-practices:
* in order to preserve existing names some benchmark suites (or packages, where benchmark suites are located) have a `HideName`
  suffix. This is used during report postprocessing to remove a name component from the final name.
  For example, `my.packageHideName.main.BenchmarkSuiteHideName.bench` will be named `my.main.bench`
* `org.jetbrains.benchmarksLauncher.Random` from `benchmarksKotlinxAdapter` subproject is used instead of `Random` from stdlib.
  Migration to stdlib `Random` should be considered, but the randomness should be kept reproducible
* `org.jetbrains.benchmarksLauncher.Blackhole` from `benchmarksKotlinxAdapter` subproject is used instead of `Blackhole` from `kotlinx-benchmark`.
  Migration should be done, but there are significant differences seriously affecting the benchmarks performance:
  * `org.jetbrains.benchmarksLauncher.Blackhole` is a thread-local, while the other is passed as an argument to the benchmarks
  * `org.jetbrains.benchmarksLauncher.Blackhole` has a single `consume(Any)` method, which forces boxing, while the other
    has several overloads with primitive types

**TODO**