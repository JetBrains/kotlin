### Project structure

Most standard library sources are located in current's directory subdirectories corresponding to a particular source set.
The only exception is Kotlin/Native-specific sources and tests that are located in [kotlin-native/runtime/main](../../kotlin-native/runtime/src/main)
and [kotlin-native/runtime/test](../../kotlin-native/runtime/test) directories.

#### Building and Testing Native Standard Library
By default, Kotlin/Native compilation is disabled, and corresponding tests and binary compatibility checks are not executed.
To enable it, all Gradle commands should be executed with `-Pkotlin.native.enabled=true` argument appended.

#### Standard Library Source Generation

Some standard library sources are generated from [templates](../tools/kotlin-stdlib-gen/src/templates). 
Such source files have a comment explicitly mentioning that, and their names are prefixed with an underscore.
Do not edit generated sources manually.
Instead, updated corresponding template files and regenerate sources by running the following Gradle task:

```
./gradlew :tools:kotlin-stdlib-gen:run
```

Check [ReadMe](./ReadMe.md) for more details.

#### Standard Library Change Checklist
- Public API should be documented and provide samples.
- Every functional change should have corresponding tests.
- The project should be successfully compiled and tests should pass.
- Changes should not break binary compatibility, unless it is a deliberate breaking change.
- A newly added API should be annotated with `@SinceKotlin` with a Kotlin version as a parameter. Usually, the value of `kotlinLanguageVersion` property from [gradle.properties](../../gradle.properties) should be used.

#### Running Tests After Changes
- After changing stdlib sources or tests, run the libraries tests.

- To execute tests for JVM, JS, and Wasm targets, run the following command:

  ```
  ./gradlew coreLibsTest
  ```
  
- To execute tests for Kotlin/Native, run the following command:

  ```
  ./gradlew :native:native.tests:stdlibTest
  ```
  
- Known/expected test outcomes:
    - On macOS, when running tests for JVM, `NumbersTest` may fail (e.g., `floatToBits`, `doubleToBits`). These failures are acceptable and are not blockers for changes that do not affect numeric conversions.

#### Standard Library Samples
- Samples are usually implemented as unit tests showing how to use a specific API.

- Samples are located in the [stdlib/samples](../stdlib/samples) directory.

- Read [stdlib/samples/ReadMe](../stdlib/samples/ReadMe.md) for more information.

#### Binary Compatibility Requirements
- All symbols constituting the public ABI are stored in dump files generated and validated by the binary compatibility validator tool:

    - For JVM, dump files are located in [binary-compatibility-validator/reference-public-api](../tools/binary-compatibility-validator/reference-public-api)
    - For all other targets, dumps files are located in [binary-compatibility-validator/klib-public-api](../tools/binary-compatibility-validator/klib-public-api)

- Dumps could be updated by running the following Gradle command:

  ```
  ./gradlew :tools:binary-compatibility-validator:cleanTest :tools:binary-compatibility-validator:test -Poverwrite.output=true
  ```
  
- If the updated dump includes new lines, the change is considered a binary compatible.

- If some lines were completely removed, the change is considered a binary incompatible.

- The change, until otherwise is required, should preserve binary compatibility.
