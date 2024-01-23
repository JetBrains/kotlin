# Swift Export Runner

## Description 

This module represents an entry point for the SwiftExport-Frontend functionality. It is implemented as a kotlin library and should be consumed by Build Tooling or IDE. 

### How to generate tests:
```bash
./gradlew :generators:sir-tests-generator:generateTests
```
this will generate tests from the input files. The input files can be found and should be placed here: `plugins/swift-export/testData`

The test expects to find the `.golden.swift`, `.golden.kt` and `.golden.h` files that contain the resulting bridges. The name of the `.golden.*` file should be the same as the name of the corresponding `.kt` file.

The project for the generator can be found here — `generators/sir-tests-generator/build.gradle.kts`

### How to run the tests:
```bash
./gradlew :native:swift:sirAllTests
```
this command will run all tests for Swift Export.
