# Kotlin-native interop

## Usage

Create file `../gradle.properties` with contents:

    llvmInstallPath=/path/to/llvm

Create a Gradle subproject somewhere under `../`, using `../InteropExample` as a template.

To generate the interop stubs and libraries and build all sources you can run
the following command from `../`:

    ./gradlew InteropExample:build

To run the example (if 'application' plugin is enabled):

    ./gradlew InteropExample:run
