# java-flight-recorder

This convention plugin enables JFR recording for test execution.

It's mainly used in `test-inputs-check-v2`, but since JFR is also a profiler, it may be useful without the undeclared inputs check. Because
of that, it's implemented as a separate convention.

## Usage

```kotlin
plugins {
    id("java-flight-recorder")
}
```

With the default settings, it will use `tests/jfr/local.jfc` or `tests/jfr/teamcity.jfc`, depending on where the tests are executed.

It will also produce `<projectDir>/build/jfr/<testTaskName>.jfr` file that you may open in IntelliJ IDEA and explore the results.

## Configuration

You customize the default settings via `javaFlightRecorder` extension, that is added to every `Test` task:

```kotlin
plugins {
    id("java-flight-recorder")
    id("project-tests-conventions")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        javaFlightRecorder {
            jfcFile.set(layout.projectDirectory.file("custom-settings.jfc"))
            jfrFile.setFrom(layout.buildDirectory.file("custom-snapshot.jfr"))
        }
    }
}
```

## Default config files

You can find the default config files in the `tests/jfr` directory.

`local.jfc`:
  - applied when tests are executed locally
  - enables stacktrace capturing for easier debugging
  - enables execution sampling; without it, IDEA won't be able to open the JFR snapshot!

`teamcity.jfc`
  - applied when tests are executed on TeamCity
  - disables stacktrace capturing for better performance

