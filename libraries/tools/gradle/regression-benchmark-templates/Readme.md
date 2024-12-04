## Description

Provides Kotlin script template to define and run [gradle-profiler](https://github.com/gradle/gradle-profiler) benchmarks.

### Writing benchmark script

This template is automatically applied to all script files with `.benchmark.kts` file extension.

The script itself should have following:
- A `@file:BenchmarkProject` annotation providing name of the project-under-benchmark,
git url to the project repo and git commit sha which should be used to run benchmarks.
- definition of gradle-profiler scenarios is written in `suite { .. }` DSL.
- provide optional git patch files that will be applied to the project before running benchmarks.

To run the actual benchmarks script either could call specific steps or just call `runAllBenchmarks()` function. 
This function calls required steps in order, runs provided benchmarks and then shows aggregated result.

Aggregated results are available in form of CSV or HTML files.
