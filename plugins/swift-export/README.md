# Swift Export Compiler Plugin

## Description 

This module represents an entry point for the SwiftExport-Frontend functionality. It is implemented as a compiler plugin, without a direct CLI interface.

## Usage Guide 

### How to build

```bash
./gradlew kotlin-swift-export-compiler-plugin:build
```

This will produce the resulting jar (with all dependency embedded) at `%PATH_TO_KOTLIN_REPO%/plugins/swift-export/build/libs/kotlin-swift-export-compiler-plugin-%VERSION%.jar`

### Example usage

Given that:
1. there is a `kotlinc` installed at `$PATH`
2. sources are located in the `app.kt` file
3. the `$PLUGIN_PATH` variable contains the path to the jar received from the ["How to build"](#How-to-build) section 

```bash
kotlinc -language-version 2.0 app.kt -Xplugin=$PLUGIN_PATH -P plugin:org.jetbrains.kotlin.swiftexport:output_dir="~/my_awesome_directory/"
```

This will produce the following file tree:
```
~/my_awesome_directory/
|_result.swift
|_result.kt
|_result.h
```

`result.kt` - contains the kotlin bridge for the resulting module. It should be compiled against the `.klib` produced by kotlinc-native in order to receive the final binary.
`result.h` - contains C-header for final binary. The `result.swift` uses this header to talk to this compiled binary.

`result.swift` - contains the resulting Swift API for the compiled Kotlin/Native library.

### Supported Options

#### Output Directory

`output_dir` — Required parameter. Determines where the resulting artifacts will be placed.

#### Named

`named` — Optional parameter. Determines the names of the resulting files. By default — `result`.

## Dev guide

### How to generate tests:
```bash
./gradlew :generators:sir-tests-generator:generateTests
```
this will generate tests from the input files. The input files can be found and should be placed here: `plugins/swift-export/testData`

The test expects to find the `.golden.swift`, `.golden.kt` and `.golden.h` files that contain the resulting bridges. The name of the `.golden.*` file should be the same as the name of the corresponding `.kt` file.

The project for the generator can be found here — `generators/sir-tests-generator/build.gradle.kts`

### How to run the tests:
```bash
./gradlew :kotlin-swift-export-compiler-plugin:test
```
OR just open `SwiftExportCompilerPluginTest` in IDEA and start them from gutter.
