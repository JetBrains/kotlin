## Swift import standalone integration tests

This directory contains infrastructure and test cases for `swift-import-standalone` module. There are two kinds of tests:
* generation: compare `swift-import-standalone` artifacts (Kotlin sources, Swift bridges and C header) against the golden data.
* execution: compile the imported Swift module together with the generated Swift bridges into a dynamic library,
  and run a Kotlin/Native executable linked against it.

A test data directory is a single Swift module: the directory name is the module name,
and all `.swift` files directly inside the directory are its sources.
Execution tests additionally contain:
* `<name>.kt`: Kotlin sources with the `main` function that calls the imported declarations;
* `<name>.out` (optional): expected stdout of the executable;
* `exitCode` (optional): expected exit code of the executable, `!0` means any non-zero exit code.

The modules are a part of the build only when Swift Import is enabled.
See [swift-import-standalone/README.md](../swift-import-standalone/README.md) for the prerequisites.

### Running tests

```
./gradlew :native:swift:swift-import-standalone-integration-tests:simple:test
```

### Updating test data

```
./gradlew :native:swift:swift-import-standalone-integration-tests:simple:test --tests "org.jetbrains.kotlin.swiftimport.standalone.test.SwiftImportWithResultValidationTest" -Pkotlin.test.update.test.data=true
```
