# Kotlin Gradle Plugin

Gradle plugin for building Kotlin projects. Supports JVM, JavaScript, WebAssembly, Native targets, and Kotlin Multiplatform.

## Module Structure

Main sources in `src/common/kotlin/org/jetbrains/kotlin/gradle/`:
- `plugin/` - Core plugin classes (`KotlinJvmPlugin`, `KotlinAndroidPlugin`, etc.)
- `tasks/` - Compilation and tooling tasks
- `targets/` - Target platform configurations (JVM, JS, Native, WASM)
- `dsl/` - Kotlin DSL extensions
- `internal/` - Internal utilities
- `incremental/` - Incremental compilation support

Gradle version-specific code in `src/gradle*/` directories.

## Related Modules

| Module | Description |
|--------|-------------|
| [`kotlin-gradle-plugin-api`](../kotlin-gradle-plugin-api) | Public API surface (binary-compatible) |
| [`kotlin-gradle-plugin-idea`](../kotlin-gradle-plugin-idea) | IDE import models |
| [`kotlin-gradle-plugin-integration-tests`](../kotlin-gradle-plugin-integration-tests) | Integration tests |
| [`kotlin-gradle-plugin-annotations`](../kotlin-gradle-plugin-annotations) | API annotations |

**Subplugins:** `kotlin-allopen`, `kotlin-noarg`, `kotlin-sam-with-receiver`, `kotlin-serialization`, `kotlin-compose-compiler`

## Build Commands

```bash
# Install to local Maven repository
./gradlew :kotlin-gradle-plugin:install

# Install subplugins
./gradlew :kotlin-allopen:install :kotlin-noarg:install :kotlin-sam-with-receiver:install
```

## Testing

### Functional Tests (in this module)

Unit-style tests using Gradle's `ProjectBuilder` API. Located in `src/functionalTest/`.

```bash
./gradlew :kotlin-gradle-plugin:functionalTest
```

Use `buildProject {}` or `buildProjectWithMPP {}` from `util/buildProject.kt` to create test projects.

### Integration Tests

End-to-end tests using Gradle TestKit. Located in [`kotlin-gradle-plugin-integration-tests`](../kotlin-gradle-plugin-integration-tests/Readme.md).

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
