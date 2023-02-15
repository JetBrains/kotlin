# degrade

`degrade` is a tool that analyses kotlin-multiplatform Gradle plugin events,
and creates shell scripts that run executed Kotlin/Native tools (compiler, cinterop)
without Gradle.

Intended to be helpful in minimizing reproducers or investigating
Kotlin/Native-related problems.

Confirmed to work with Kotlin versions from 1.8.10 to 1.9.0-Beta
Probably doesn't work on Windows.

## Usage

```
degrade <your usual Gradle invocation>
```

for example, in a Gradle project directory
```
degrade ./gradlew build
```

The tool runs Gradle build, analyses certain events during the run,
and generates shell scripts in the `degrade` directory at the project root.
It emits a script per Kotlin/Native task, and also `rerun-all.sh` and
`rerun-failed.sh`.
