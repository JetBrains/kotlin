# proejct-test-convention

## Description

This plugin adds dependencies to the test tasks, building the required jars and tracking them as inputs, but it passes the absolute paths to the test as systemProperties. 

This plugin is key for:
- make tests cacheable, avoiding execution if no input changed
- allow Gradle Predictive Test Selection to understand the code changes and have an accurate prediction

## Usage

Apply it: 
```kotlin
plugins {
    id("project-tests-convention")
}
```

Add dependencies to the test classpath:
```kotlin
projectTests {
    withJvmStdlibAndReflect()
    withStdlibCommon()
    withTestJar()
    withAnnotations()
    ...
}
```

## Test dry runs

Use `-Pkotlin.build.test.dry.run=true` or set `KOTLIN_BUILD_TEST_DRY_RUN=true` to enable
dry-run mode for test tasks. The Gradle property takes precedence over the environment variable.

JVM test tasks discover tests and generate reports without executing the tests. Other test tasks
(including JS, Wasm, and Native test executables) skip execution because they do not support Gradle's test
discovery dry run. Dependencies needed to compile and discover tests are still built.
Runtime-only compiler builds and the codebase checks' `compileAll` dependency are omitted in dry-run mode.

To select all test tasks across the project, run this from the repository root:

```shell
./gradlew allTestTasks -Pkotlin.build.test.dry.run=true
```

Use the unqualified task name `allTestTasks` so Gradle selects it in every subproject.
`:allTestTasks` selects only the root project's task. Each project's task depends on all of its
`AbstractTestTask` tasks, including custom test tasks with names other than `test`.

The command covers projects enabled by the current build configuration. Add
`-Pkotlin.native.enabled=true` to include Kotlin/Native projects. Existing task enablement and
host platform restrictions still apply.
