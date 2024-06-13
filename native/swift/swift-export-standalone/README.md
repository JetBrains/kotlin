# Swift Export Standalone

`swift-export-standalone` module is a high-level entry point for running Swift export against given Kotlin module.
It encapsulates all implementation details like Analysis API, SIR, and others under a simple "take files return files" API, and is intended
to be used in standalone scenarios like Kotlin Gradle Plugin. 

Note that this module does not embed its dependencies. 
If you would like to use fully self-contained artifact please refer to [`swift-export-embeddable`](../swift-export-embeddable) module.

## Testing

### How to generate tests

The test data is stored under [`testData`](testData) directory.
When adding a new test case, don't forget to update the generated unit tests by running

```bash
gradle :generators:sir-tests-generator:generateTests
```

### How to run the tests

```bash
gradle :native:swift:swift-export-standalone:check
```
