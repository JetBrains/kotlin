# Kotlin Gradle Plugin Integration Tests

Integration tests for the Kotlin Gradle Plugin using Gradle TestKit.

## Running Tests

```bash
# Platform-specific tests (parallel execution)
./gradlew :kotlin-gradle-plugin-integration-tests:kgpJvmTests
./gradlew :kotlin-gradle-plugin-integration-tests:kgpJsTests
./gradlew :kotlin-gradle-plugin-integration-tests:kgpMppTests
./gradlew :kotlin-gradle-plugin-integration-tests:kgpNativeTests
./gradlew :kotlin-gradle-plugin-integration-tests:kgpAndroidTests
./gradlew :kotlin-gradle-plugin-integration-tests:kgpOtherTests

# Daemon tests (sequential execution)
./gradlew :kotlin-gradle-plugin-integration-tests:kgpDaemonTests

# All tests except daemon tests
./gradlew :kotlin-gradle-plugin-integration-tests:kgpAllParallelTests

# Run specific test class
./gradlew :kotlin-gradle-plugin-integration-tests:kgpJvmTests --tests "org.jetbrains.kotlin.gradle.SomeIT"
```

## Writing Tests

### Base Class and Tags

Extend `KGPBaseTest` and annotate with appropriate tag:
- `@JvmGradlePluginTests` - Kotlin/JVM tests
- `@JsGradlePluginTests` - Kotlin/JS tests
- `@MppGradlePluginTests` - Multiplatform tests
- `@NativeGradlePluginTests` - Kotlin/Native tests
- `@AndroidGradlePluginTests` - Android tests
- `@DaemonsGradlePluginTests` - Daemon tests (run sequentially)
- `@OtherGradlePluginTests` - kapt, allopen, serialization, etc.

### Test DSL

```kotlin
@GradleTest
@JvmGradlePluginTests
fun testSomething(gradleVersion: GradleVersion) {
    project("simpleProject", gradleVersion) {
        build("assemble") {
            assertTasksExecuted(":compileKotlin")
        }
    }
}
```

### Build Script Injections

Inject code directly into test project build scripts:

```kotlin
project("empty", gradleVersion) {
    plugins {
        kotlin("jvm")
    }
    buildScriptInjection {
        // Code executed in build.gradle.kts during evaluation
        project.tasks.register("myTask") { }
    }
    build("myTask")
}
```

## Project Structure

- `src/test/kotlin/.../gradle/` - Test classes organized by feature
- `src/test/kotlin/.../gradle/testbase/` - Test infrastructure (DSL, assertions, helpers)
- [`src/test/resources/testProject/`](src/test/resources/testProject) - Test project templates

## Key Files

| File | Purpose |
|------|---------|
| [`testbase/KGPBaseTest.kt`](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/KGPBaseTest.kt) | Base class for all tests |
| [`testbase/testDsl.kt`](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/testDsl.kt) | `project()`, `build()`, `buildAndFail()` |
| [`testbase/testTags.kt`](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/testTags.kt) | Test tag annotations |
| [`testbase/tasksAssertions.kt`](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/tasksAssertions.kt) | `assertTasksExecuted()`, etc. |
| [`testbase/outputAssertions.kt`](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/outputAssertions.kt) | `assertOutputContains()`, etc. |
| [`testbase/fileAssertions.kt`](src/test/kotlin/org/jetbrains/kotlin/gradle/testbase/fileAssertions.kt) | File content assertions |

## Related Modules

- [`kotlin-gradle-plugin`](../kotlin-gradle-plugin) - Main plugin (has functional tests)
- [`kotlin-gradle-plugin-api`](../kotlin-gradle-plugin-api) - Public API
