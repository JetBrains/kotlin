# Guide to using LitmusKt in Kotlin/Native

This guide is not intended to be complete. Instead, the key points for running and maintaining the LitmusKt integration should be outlined here.

## Basics

* Only `litmuskt-core` and `litmuskt-testsuite` parts are currently relevant. Things like JCStress interop and a CLI are currently omitted from this integration.
* The integration is encapsulated into `:litmuskt:repo-tests` subproject. Some necessary _native_ utils are located in `:litmuskt:repo-utils` subproject.
* To run a test, do the following:
    * Write the test in `:repo-utils` as per normal LitmusKt guide. See [here](repo-utils/src/nativeMain/kotlin/org/jetbrains/litmuskt/extratests/RepoTest.kt) for a sample test
    * Add a wrapper for it into `:repo-tests` using existing wrappers as reference
      * Note that nothing is stopping you from writing a new test directly into `testData`. However, it is more practical to write it inside a normal Gradle subproject, rather than quasi-Kotlin files
    * Run `./gradlew :native:native.tests:generateTests`
    * Then run `./gradlew :litmuskt:repo-tests:nativeTest`
  
## Notes

* The `// IGNORE_NATIVE: ...` lines in all test files are a workaround (for unknown reasons, builds with certain types of cache always fail because they cannot link the cinterop klib).

## Updating LitmusKt version

1. Update library versions in [repo-utils](repo-utils/build.gradle.kts) and [repo-tests](repo-tests/build.gradle.kts)
2. Update the [checksum file](../../gradle/verification-metadata.xml). See the relevant [Kotlin README section](https://github.com/JetBrains/kotlin?tab=readme-ov-file#dependency-verification)
