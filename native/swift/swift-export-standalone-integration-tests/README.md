## Swift export standalone integration tests

This directory contains infrastructure and test cases for `swift-export-standalone` module. There are two kinds of tests:
* generation: compare `swift-export-standalone` artifacts against the golden data. 
* execution: run Swift tests against the generated API. 

And two groups:
* `simple`: synthetic tests to test specific features of Swift export.
* `external`: test behavior of Swift export against "real-world" libraries.

### Updating test data

These tests tend to be large, and manual test data for generation tests update might be challenging. 
To update test data in bulk, use the following command:
```
./gradlew :native:swift:swift-export-standalone-integration-tests:external:test --tests "org.jetbrains.kotlin.swiftexport.standalone.test.ExternalProjectGenerationTests" -Pkotlin.test.update.test.data=true
./gradlew :native:swift:swift-export-standalone-integration-tests:simple:test --tests "org.jetbrains.kotlin.swiftexport.standalone.test.KlibBasedSwiftExportRunnerTest" -Pkotlin.test.update.test.data=true
```