## Running tests

For tests, use `./gradlew :native:native.tests:test`.

To execute several tests only, use `./gradlew :native:native.tests:test --tests "org.jetbrains.kotlin.konan.blackboxtest.NativeCodegenBoxTestGenerated*"`

To re-generate tests, use `./gradlew :native:native.tests:generateTests`

For more details see [Testing](../../kotlin-native/HACKING.md#Testing).
