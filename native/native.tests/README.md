## Running tests

* To run all tests, use `./gradlew :native:native.tests:test`. Please note, this Gradle task is available only in development environment and it not available at CI server.
* To execute certain tests only, use the appropriate Gradle tasks. Example: `./gradlew :native:native.tests:codegenBoxTest`
* To re-generate tests, use `./gradlew :native:native.tests:generateTests`

For more details see [Testing](../../kotlin-native/HACKING.md#Testing).
