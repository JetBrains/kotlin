# WASM Benchmarking Infrastructure

This document describes the benchmarking infrastructure for WASM tests, which is built on top of the existing test infrastructure.

## Overview

The benchmarking suite allows you to measure the performance of WASM code by running existing box tests with performance measurement capabilities. It is based on `SpecBoxRunner` and uses the WebAssembly reference interpreter for consistent, deterministic performance measurements.

## Files

### Core Files

- **`wasiBenchmarkRun.kt`**: The benchmark runner that measures performance of `box()` functions
  - Uses `kotlin.time.TimeSource.Monotonic` for accurate timing
  - Performs warmup iterations (5 by default) to stabilize JIT compilation
  - Runs benchmark iterations (100 by default) and collects timing data
  - Calculates and reports statistics: min, max, average, and median execution times
  - Validates that each iteration returns "OK" (same as regular tests)

- **`WasmAdditionalSourceProvider.kt`**: Contains the source provider infrastructure
  - `WasmWasiBenchmarkHelperSourceProvider`: Injects `wasiBenchmarkRun.kt` into tests with `box()` functions
  - Handles package name resolution automatically

- **`AbstractFirWasmTest.kt`**: Contains the benchmark test base classes
  - `AbstractFirWasmWasiBenchmarkTest`: Base class for WASI benchmark tests
  - `AbstractFirWasmSpecBenchmarkTest`: Base class for spec benchmark tests
  - `AbstractFirWasmWasiCodegenBenchmarkTest`: Concrete class for codegen benchmarks
  - `AbstractFirWasmSpecCodegenBenchmarkTest`: Concrete class for spec codegen benchmarks

- **`WasiBenchmarkRunner.kt`**: Custom test handler that executes benchmarks and writes results
  - Based on `SpecBoxRunner` pattern for reference interpreter execution
  - Extends `AbstractWasmArtifactsCollector` to handle WASM artifacts
  - Executes benchmarks on the WebAssembly reference interpreter only
  - Captures stdout output containing benchmark statistics
  - Writes results to `benchmark_results.txt` in the project root directory

## Usage

### Creating Benchmark Tests

To create a benchmark test class, extend one of the benchmark base classes:

```kotlin
// Example: Benchmark coroutine tests
class FirWasmSpecCodegenBenchmarkCoroutineTest : AbstractFirWasmSpecCodegenBenchmarkTest("benchmark/spec/coroutines")

// Example: Benchmark specific codegen tests
class FirWasmWasiCodegenBenchmarkMyFeatureTest : AbstractFirWasmWasiCodegenBenchmarkTest("benchmark/wasi/myfeature")
```

### Running Benchmarks

Benchmark tests work with any existing test that has a `box()` function returning a String. The infrastructure will:

1. Automatically inject `wasiBenchmarkRun.kt` as an additional source file
2. Adjust package names to match the test's package
3. Compile the test with the benchmark runner
4. Execute the benchmark and collect performance data

### Output Format

The benchmark runner outputs statistics in the following format:

```
Benchmark Results (nanoseconds):
  Iterations: 100
  Min: 1234567
  Max: 2345678
  Average: 1567890
  Median: 1545678
```

### Benchmark Report File

All benchmark results are automatically written to a single report file located at:

```
<project-root>/benchmark_results.txt
```

The report file contains:
- **Timestamp**: When the benchmark was executed
- **Test Path**: Full path to the test file
- **Mode**: Compilation mode (dev, dce, or optimized)
- **VM Results**: Benchmark statistics from the reference interpreter
- **Raw Output**: Complete stdout from the benchmark execution

The report file is appended to with each benchmark run, creating a cumulative history of all benchmark executions. Each benchmark entry is separated by dividers for easy reading.

Example report format:
```
================================================================================
Benchmark Report
Timestamp: 2026-01-16T15:20:00.123456
Test: /path/to/test/file.kt
Mode: dev
================================================================================
VM: Ref, Mode: dev
Benchmark Results (nanoseconds):
  Iterations: 100
  Min: 1234567
  Max: 2345678
  Average: 1567890
  Median: 1545678
--------------------------------------------------------------------------------

```

### Customizing Benchmark Parameters

To customize warmup or benchmark iterations, modify the constants in `wasiBenchmarkRun.kt`:

```kotlin
val warmupIterations = 5        // Number of warmup runs
val benchmarkIterations = 100   // Number of measured runs
```

## Architecture

The benchmarking infrastructure is based on the SpecBoxRunner pattern:

```
wasiBoxTestRun.kt (WasiBoxRunner)    wasiBenchmarkRun.kt (WasiBenchmarkRunner)
       ↓                                     ↓
WasmWasiBoxTestHelperSourceProvider  WasmWasiBenchmarkHelperSourceProvider
       ↓                                     ↓
AbstractFirWasmWasiTest              AbstractFirWasmSpecBenchmarkTest
       ↓                                     ↓
AbstractFirWasmWasiCodegenBoxTest    AbstractFirWasmSpecCodegenBenchmarkTest

SpecBoxRunner (ReferenceInterpreter) ← WasiBenchmarkRunner
```

The benchmarking infrastructure uses SpecBoxRunner's pattern for consistent, deterministic measurements using the WebAssembly reference interpreter.

## Implementation Details

### Timing Mechanism

The benchmark uses `kotlin.time.TimeSource.Monotonic` which:
- Provides monotonic time measurements (no clock adjustments)
- Uses WASI's `clock_time_get` with `CLOCK_ID_MONOTONIC` under the hood
- Returns nanosecond precision timing data

### Warmup Phase

The warmup phase is important for:
- Stabilizing JIT compilation effects
- Warming up caches
- Ensuring consistent measurements

### Statistical Analysis

The benchmark collects multiple samples and calculates:
- **Min**: Best case performance
- **Max**: Worst case performance
- **Average**: Mean execution time
- **Median**: Middle value (more robust to outliers than average)

## Example

To benchmark existing coroutine tests:

1. Create a test class:
```kotlin
class FirWasmSpecCodegenBenchmarkCoroutineTest : 
    AbstractFirWasmSpecCodegenBenchmarkTest("benchmark/spec/coroutines")
```

2. Run the test class - it will automatically find and benchmark all tests with `box()` functions in the coroutine test directory

3. Review the output for performance metrics

## Notes

- Benchmark tests validate correctness (box() must return "OK") before recording performance
- Failed tests return -1 and exit with code 1
- The infrastructure reuses existing test data - no need to duplicate test files
- Benchmark results are printed to stdout and automatically written to `benchmark_results.txt` in the project root
- The report file is appended to with each run, preserving historical benchmark data
- Each test is benchmarked in multiple modes (dev, dce, optimized) using the WebAssembly reference interpreter
- The reference interpreter provides consistent, deterministic performance measurements without JIT compilation effects
