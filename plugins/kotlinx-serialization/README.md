# Kotlinx serialization compiler plugin

This folder contains compiler plugin counterpart to [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) library:
source code for all compiler-related plugin parts, including code resolution, code generation, diagnostics, and tests.
Gradle and Maven plugins can be found in the `libraries` folder.
The IntelliJ IDEA part (which contains only specific inspections and quickfixes) is located
[inside the IntelliJ IDEA Kotlin plugin in the IntelliJ Community monorepo](https://github.com/JetBrains/intellij-community/tree/master/plugins/kotlin/compiler-plugins/kotlinx-serialization).

## Brief overview

Plugin consists of five parts:

1.`backend` — responsible for IR code generation as well as old JVM & JS compiler backends (to be removed in the future).
2. `k1` — Code resolution and diagnostics for the Kotlin frontend.
3. `k2` — Code resolution and diagnostics for the new K2 Kotlin compiler.
4. `cli` — extension points that allow the plugin to be loaded with `-Xplugin` Kotlin CLI compiler argument.
5. `common` — common declarations for other parts.

Tests and test data are common for all parts and located directly in this module (see `testData` and `tests-gen` folders).

## Building and contributing

### Prerequisites

Before all, it is recommended to read root `README.md` and ensure you have all the necessary things installed (you don't need JDK6 to work with this plugin).

### Installing locally

Just run `./gradlew dist install` to get a fresh Kotlin compiler and kotlinx.serialization plugin in your Maven local with `1.x.255-SNAPSHOT` versions.
Installing the serialization plugin alone is not recommended, as it may not be binary compatible with the latest published version of Kotlin.

### Working with tests

As in most Kotlin project modules, tests are generated based on test data.
Tests are located in `test-gen` folder and can be run using the green arrow on the IDE gutter or with standard
`./gradlew :kotlinx-serialization-compiler-plugin:test` task.
To add a new test, add an appropriate file to `testData` folder and then re-generate tests with `./gradlew :kotlinx-serialization-compiler-plugin:generateTests`.

### Building maven plugin

A Gradle plugin is installed during project-wide `./gradlew dist install`. In rare cases when you need a snapshot Maven plugin, follow these steps:
Make all prerequisites from `$kotlin_root/libraries/README.md` for Maven projects. Go to `$kotlin_root/libraries/tools/kotlin-maven-serialization`. Run `mvn install`.

### Contributing

Follow the common [Kotlin's contribution guidelines](../../docs/contributing.md).
In general, create an issue in Kotlin's YouTrack or [kotlinx.serialization's GitHub](https://github.com/Kotlin/kotlinx.serialization/issues/new/choose) to discuss suggested changes beforehand. 


