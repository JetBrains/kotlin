# Guide to using LitmusKt in Kotlin/Native

This guide is not intended to be complete. Instead, the key points for running and maintaining the LitmusKt integration should be outlined here.

## Basics

* Only `litmuskt-core` and `litmuskt-testsuite` parts are currently relevant. Things like JCStress interop and a CLI are currently omitted from this integration.
* The integration is encapsulated into `:native:native.tests:litmus-tests` subproject.
* An example custom test can be found [here](testData/standalone/Sample.kt). It consists of
  * `runTest` wrapper that configures LitmusKt test running for Native test infrastructure
  * Litmus test itself defined by `litmusTest`
  * And a wrapper that calls `runTest` on the Litmus test
* To run the tests, do the following:
    * Run `./gradlew :native:native.tests:generateTests`
    * Then run `./gradlew :native:native.tests:litmus-tests:check -Pkotlin.internal.native.test.optimizationMode=OPT`
  
## Notes

* The `// IGNORE_NATIVE: ...` lines in all test files are a workaround (for unknown reasons, builds with certain types of cache always fail because they cannot link the cinterop klib).
* Currently, the tests can only be run in optimized mode.

## Updating LitmusKt version

1. Update library version `litmusKtVersion` in [the build script](build.gradle.kts)
2. Update the [checksum file](../../../gradle/verification-metadata.xml). See the relevant [Kotlin README section](https://github.com/JetBrains/kotlin?tab=readme-ov-file#dependency-verification)
