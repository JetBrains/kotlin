# Test Data Manager Convention Plugin

Gradle convention plugins for managing test data files in the Kotlin project.

## Overview

The test data manager provides automated checking and updating of test data files.
It runs tests in a special mode that compares actual output against expected files and can update
them when differences are found.

## Plugins

### `test-data-manager` (Module Plugin)

Apply to modules that have managed test data:

```kotlin
plugins {
    id("test-data-manager")
}
```

This registers two tasks in the module:

- **`manageTestData`** — supports both check and update modes. Options accepted via `--option` CLI flags.
- **`updateTestData`** — always runs in update mode. Options accepted only via `-P` Gradle properties; configuration cache stays valid when option values change between runs (see [Configuration Cache](#configuration-cache)).

### `test-data-manager-root` (Root Plugin)

Apply to the root project to get global orchestration:

```kotlin
plugins {
    id("test-data-manager-root")
}
```

This registers a `manageTestDataGlobally` task that runs all module tasks.

## Usage

### Per-Module Execution

Run on a single module:

```bash
# Check mode (default) - fails if test data doesn't match
./gradlew :analysis:analysis-api-fir:manageTestData

# Update mode - updates test data files
./gradlew :analysis:analysis-api-fir:manageTestData --mode=update

# Filter by test data path
./gradlew :analysis:analysis-api-fir:manageTestData --mode=update --test-data-path=testData/myTest.kt

# Filter by test class pattern
./gradlew :analysis:analysis-api-fir:manageTestData --mode=update --test-class-pattern=.*Fir.*
```

### Global Execution

Run across all modules with the plugin:

```bash
# Check all test data
./gradlew manageTestDataGlobally

# Update all test data
./gradlew manageTestDataGlobally --mode=update

# Filter by path or pattern (applies to all modules)
./gradlew manageTestDataGlobally --mode=update --test-data-path=testData/myTest.kt

# Run only golden tests (skip variant-specific tests)
./gradlew manageTestDataGlobally --mode=update --golden-only

# Incremental update — only run variant tests for changed golden paths
./gradlew manageTestDataGlobally --mode=update --incremental
```

### Module Filtering

Filter which modules to run in global mode:

```bash
# Single module
./gradlew manageTestDataGlobally -Porg.jetbrains.kotlin.testDataManager.options.module=:analysis:analysis-api-fir

# Multiple modules (comma-separated)
./gradlew manageTestDataGlobally -Porg.jetbrains.kotlin.testDataManager.options.module=:analysis:analysis-api-fir,:analysis:analysis-api-standalone
```

## Available Options

View all options:

```bash
./gradlew help --task manageTestDataGlobally
./gradlew help --task :analysis:analysis-api-fir:manageTestData
```

| Option                 | Description                                                                    |
|------------------------|--------------------------------------------------------------------------------|
| `--mode`               | `check` (default) or `update`                                                  |
| `--test-data-path`     | Filter tests by test data file path                                            |
| `--test-class-pattern` | Filter tests by class name regex                                               |
| `--golden-only`        | Run only golden tests (empty variant chain)                                    |
| `--incremental`        | Only run variant tests for paths changed in first group (with `--mode=update`) |

## `updateTestData` — CC-friendly update mode

`manageTestData`'s `--option` CLI flags are part of Gradle's configuration-cache key, so iterating
on `--test-data-path` causes a full reconfiguration (often 1–2 minutes) on every value change. The
`updateTestData` task addresses this:

- **Mode is fixed to `update`** — no `--mode` flag.
- **All options are passed via `-P` Gradle properties.** The values are read only at execution time
  and forwarded as `-D` system properties to the test runner JVM, so changing them between runs does
  **not** invalidate the configuration cache.
- **No global orchestrator task.** Use Gradle's task-name matching (`./gradlew updateTestData`) to
  run across all modules with the plugin, or supply explicit task paths to filter modules.

### Options

| Gradle property                                                       | Effect                                          |
|-----------------------------------------------------------------------|-------------------------------------------------|
| `org.jetbrains.kotlin.testDataManager.options.testDataPath`           | Comma-separated test data paths (dir or file)   |
| `org.jetbrains.kotlin.testDataManager.options.testClassPattern`       | Regex pattern for test class names              |
| `org.jetbrains.kotlin.testDataManager.options.goldenOnly`             | Run only golden tests (empty variant chain)     |
| `org.jetbrains.kotlin.testDataManager.options.incremental`            | Only run variant tests for changed golden paths |

### Examples

```bash
# Update a single test data file in one module — fast iteration without reconfiguration
./gradlew :analysis:analysis-api-fir:updateTestData \
    -Porg.jetbrains.kotlin.testDataManager.options.testDataPath=path/to/file.kt

# Update across all modules with the plugin
./gradlew updateTestData

# Filter by class pattern across all modules
./gradlew updateTestData \
    -Porg.jetbrains.kotlin.testDataManager.options.testClassPattern=.*Fir.*

# Limit to a subset of modules using task paths
./gradlew :analysis:analysis-api-fir:updateTestData :analysis:stubs:updateTestData
```

### Configuration Cache

With `--configuration-cache` enabled, two consecutive `updateTestData` runs that differ only in
the values of the `-P` options listed above will reuse the same CC entry — Gradle prints
`Reusing configuration cache.` and skips reconfiguration entirely. The `manageTestData` task does
not have this property because its CLI `--option` values are tracked as task inputs.

### Trade-off: not Gradle-cacheable

The same mechanism that keeps the CC stable — not declaring options as `@Input` properties —
also hides them from Gradle's task-identity machinery. As a result, `updateTestData` is **never**
UP-TO-DATE and its result is never restored from the build cache: the test runner is invoked on
every invocation. In practice this matches `manageTestData`'s behavior (both are `JavaExec` tasks
with no declared outputs that always re-run), but the choice is permanent and intentional here —
input-tracking the options would undo the CC benefit. Use `manageTestData` if you ever need
Gradle to reason about the task's inputs.

## Execution Order

Module ordering is determined by `mustRunAfter` dependencies inherited from each module's
`test` task. This ensures that golden modules (which establish baseline `.txt` files) run
before dependent modules (which may create prefixed variants like `.descriptors.txt`).

To configure ordering, set up `mustRunAfter` on your module's test task in `build.gradle.kts`:

```kotlin
tasks.named<Test>("test") {
    mustRunAfter(":analysis:analysis-api-fir:test")
}
```

The `manageTestData` task automatically inherits this ordering.

## See Also

- [analysis/test-data-manager](../../../analysis/test-data-manager/README.md) — Implementation module with `ManagedTest` interface and `assertEqualsToTestDataFile()` assertions
