# Swift Import Standalone

`swift-import-standalone` module is an entry point for importing a Swift module into Kotlin.
It analyzes the Swift sources of a single module with [swiftextract](https://github.com/swiftlang/swift-java)
and translates the result into SIR.

Currently, only top-level functions without parameters are supported.

## Prerequisites

The module depends on `org.swift.swiftkit:swiftextract`, which is not published to any remote repository yet.
It is resolved from the local Maven repository (`~/.m2`), so you have to build and publish it yourself.

Requirements:

* macOS on Apple Silicon (the jar bundles `osx-aarch_64` dylibs)
* Swift 6.1 or newer
* JDK 25

### Publishing swiftextract to the local Maven repository

The jar is produced by the `build-swiftextract-jar.sh` script from the `swift-extract-jar-maven` branch of the swift-java fork:

```bash
git clone https://github.com/RandVid/swift-java-kotlin.git
cd swift-java-kotlin
git checkout swift-extract-jar-maven
./build-swiftextract-jar.sh
```

### Enabling the module

The module is excluded from the build by default. To enable it, add the following to `local.properties` in the repository root:

```properties
swiftimport.enabled=true
```

The checksums of swiftextract are not committed, because every locally built jar is different.
On the first build after enabling the module, generate them once:

```bash
./gradlew --write-verification-metadata sha256 :native:swift:swift-import-standalone:compileTestKotlin
```

This adds the `org.swift.swiftkit` entries (and possibly entries for other artifacts resolved by the build)
to [`verification-metadata.xml`](../../../gradle/verification-metadata.xml). **Do not commit these changes.**
Rerun the command whenever you rebuild or republish swiftextract.

## Testing

### How to run the tests

```bash
./gradlew :native:swift:swift-import-standalone:test
```
