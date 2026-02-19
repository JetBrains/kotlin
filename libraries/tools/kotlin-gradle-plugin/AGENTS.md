# Kotlin Gradle Plugin

Gradle plugin for building Kotlin projects. Supports JVM, JavaScript, WebAssembly, Native targets, and Kotlin Multiplatform.

## Module Structure

Main sources in [`src/common/kotlin/org/jetbrains/kotlin/gradle/`](src/common/kotlin/org/jetbrains/kotlin/gradle):
- [`plugin/`](src/common/kotlin/org/jetbrains/kotlin/gradle/plugin) - Core plugin classes (`KotlinJvmPlugin`, `KotlinAndroidPlugin`, etc.)
- [`tasks/`](src/common/kotlin/org/jetbrains/kotlin/gradle/tasks) - Compilation and tooling tasks
- [`targets/`](src/common/kotlin/org/jetbrains/kotlin/gradle/targets) - Target platform configurations (JVM, JS, Native, WASM)
- [`dsl/`](src/common/kotlin/org/jetbrains/kotlin/gradle/dsl) - Kotlin DSL extensions
- [`internal/`](src/common/kotlin/org/jetbrains/kotlin/gradle/internal) - Internal utilities
- [`incremental/`](src/common/kotlin/org/jetbrains/kotlin/gradle/incremental) - Incremental compilation support

Gradle version-specific code in `src/gradle*/` directories.

## Related Modules

| Module | Description |
|--------|-------------|
| [`kotlin-gradle-plugin-api`](../kotlin-gradle-plugin-api) | Public API surface (binary-compatible) |
| [`kotlin-gradle-plugin-idea`](../kotlin-gradle-plugin-idea) | IDE import models |
| [`kotlin-gradle-plugin-integration-tests`](../kotlin-gradle-plugin-integration-tests) | Integration tests |
| [`kotlin-gradle-plugin-annotations`](../kotlin-gradle-plugin-annotations) | API annotations |

## Build Commands

```bash
# Install to local Maven repository
./gradlew :kotlin-gradle-plugin:install

# Install subplugins
./gradlew :kotlin-allopen:install :kotlin-noarg:install :kotlin-sam-with-receiver:install
```

## Testing

### When to Use Which Tests

- **Functional tests** - for configuration state checks (verifying tasks, extensions, properties are configured correctly)
- **Integration tests** - for everything else (actual builds, compilation, task execution, file outputs)

### Functional Tests (in this module)

Unit-style tests using Gradle's `ProjectBuilder` API. Located in [`src/functionalTest/`](src/functionalTest).

```bash
./gradlew :kotlin-gradle-plugin:functionalTest
```

Use `buildProject {}` or `buildProjectWithMPP {}` from [`util/buildProject.kt`](src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/util/buildProject.kt) to create test projects.

### Integration Tests

End-to-end tests using Gradle TestKit. Located in [`kotlin-gradle-plugin-integration-tests`](../kotlin-gradle-plugin-integration-tests).

```bash
# Run all integration tests
./gradlew :kotlin-gradle-plugin-integration-tests:check

# Platform-specific tests
./gradlew :kotlin-gradle-plugin-integration-tests:kgpJvmTests
./gradlew :kotlin-gradle-plugin-integration-tests:kgpMppTests
./gradlew :kotlin-gradle-plugin-integration-tests:kgpNativeTests
```

## API Annotations

- `@ExperimentalKotlinGradlePluginApi` - unstable public API
- `@InternalKotlinGradlePluginApi` - internal API, no compatibility guarantees
