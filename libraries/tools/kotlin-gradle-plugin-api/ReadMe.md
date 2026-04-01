## Gradle Plugin Api

Lightweight module defining the API surface of the Kotlin Gradle Plugin. 

### Binary Compatibility Validation

The public API surface of this module is checked for stability
using the [binary compatibility validator](https://github.com/Kotlin/binary-compatibility-validator/) plugin
to prevent accidental public API changes.

You can execute public API validation by running `apiCheck` task (also executed when `check` task runs).

In order to overwrite the reference API snapshot, you can launch `apiDump` task. 

# Generated files

This module generates the `DisableCacheInKotlinVersion.kt` file. This file provides a type-safe sealed class with object constants for all Kotlin versions defined in `native-cache-kotlin-versions.txt`, allowing for unforgeable, compile-time version checks.

The generator task (`:kotlin-gradle-plugin-api:generateKotlinVersionConstant`) updates the `native-cache-kotlin-versions.txt` file based on the current project version and regenerates the `DisableCacheInKotlinVersion.kt` file.

When updating the project to a new Kotlin version (e.g., `2.4.0`), you must run the generator and commit the resulting changes: `./gradlew :kotlin-gradle-plugin-api:generateKotlinVersionConstant`

Please also remember to regenerate the `./gradlew :kotlin-gradle-plugin-api:apiDump`

