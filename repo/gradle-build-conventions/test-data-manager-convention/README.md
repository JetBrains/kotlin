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

This registers a `manageTestData` task in the module.

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
