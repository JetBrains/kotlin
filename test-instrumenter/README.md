# test-instrumenter

A Java Agent for instrumenting tests, mainly used for undeclared inputs checking (see `test-inputs-check-v2`).

## Debugging

If you want to see debug logs and stack traces from the JVM, add this line to your `local.properties`:

```
test.instrumenter.debug=true
```

## System properties

The following system properties are recognized:

| System property                          | Description                                       | Source                 |
|------------------------------------------|---------------------------------------------------|------------------------|
| `test.instrumenter.debug`                | Enable/disable debug logging and JVM stack traces | `local.properties`     |
| `test.instrumenter.inputs.check.enabled` | Enable/disable inputs checking instrumentation    | `test-inputs-check-v2` |
| `test.instrumenter.root.dir`             | Root dir of kotlin.git                            | `test-inputs-check-v2` |
| `test.instrumenter.build.dir`            | Build dir of the project executing tests          | `test-inputs-check-v2` |
| `test.instrumenter.declared.inputs.file` | Path to file containg list of declared inputs     | `test-inputs-check-v2` |

## Benchmarking

The performance of the `UndeclaredInputsGuard` has been assessed via JMH microbenchmark `UndeclaredInputsBenchmark`.

> [!IMPORTANT]
> After making any modifications in the `UndeclaredInputsGuard`, please re-run the benchmark 
> and compare your performance to the baseline!

### Parameters
- every file exists on the disk
- 100k file paths to check (every file exists on the disk)
  - 30% of "normal" declared inputs
  - 4% of declared inputs with non-canonical paths
  - 11% of "normal" undeclared inputs
  - 5% of already-detected undeclared inputs
  - 4% of undeclared inputs with non-canonical paths
  - 1% of nulls
  - 20% of files outside the root dir
  - 20% of files inside the build dir
  - 5% directories

### Results

| Scenario                                         | [Mode Cnt Score Error Units]        |
|--------------------------------------------------|-------------------------------------|
| all optimizations                                | avgt   25  91.095 ± 9.472  ms/op    |
| without check for `.` and `..`                   | avgt   25  84.452 ± 3.188  ms/op    |
| `!isDirectory()` before `isUndeclaredInput()`    | avgt    5  24.564 ± 2.801  ms/op    |
| without `canonicalFile.equals(file)`             | avgt    5  28.303 ± 3.928  ms/op    |
| `getCanonicalFile()` always invoked              | avgt    5  1240.139 ± 75.544  ms/op |
| without `undeclaredInputs.contains(path)`        | avgt    5  448.375 ± 19.232  ms/op  |
| `!declaredInputs.contains(file.getPath())` first | avgt    5  98.579 ± 15.168  ms/op   |
| `ConcurrentSkipListSet`                          | avgt    5  47.507 ± 3.702  ms/op    |

### Interpretation

1. The instrumentation for undeclared inputs checking adds ~27 ms overhead per 100k accessed files
2. Some optimizations are either unnecessary, or the benchmark does not reflect real life close enough
