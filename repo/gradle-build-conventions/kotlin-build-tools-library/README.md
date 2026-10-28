# kotlin-build-tools-library

This convention plugin sets up a Kotlin Build Tools library: one of the build tool independent libraries under
`build-tools/libraries` that Kotlin build tool integrations (KGP, Maven, JPS, kotlin-toolchain,
Bazel rules) share.

It applies `common-configuration`, `kotlin("jvm")`, `org.jetbrains.kotlinx.binary-compatibility-validator`
and `test-inputs-check`, and then:

- publishes under the repository-wide group `org.jetbrains.kotlin` as `build-tools-<project name>`;
- compiles the way KGP does (`configureKotlinCompileTasksGradleCompatibility()`), with the stdlib as
  `compileOnly`, since the integration that loads the library provides it;
- turns on explicit API mode;
- configures publishing, taking the POM description from the project's `description`;
- configures JUnit 5 tests through the repository's `projectTests` factory, which attaches the
  instrumenting agent that `test-inputs-check` needs;
- dumps the public API from the jar into `api/<name>.api`.

## Usage

```kotlin
plugins {
    id("kotlin-build-tools-library")
}

description = "Kotlin Build Tools <what the library does>"

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api")) // if the library uses BTA
}
```

The rules for these libraries are in
[build-tools/libraries/AGENTS.md](../../../build-tools/libraries/AGENTS.md).

## Implementation notes

- `common-configuration` is applied with `apply(plugin = ...)` in the script body, not in `plugins {}`.
  Generating accessors for a plugin in `plugins {}` applies it to a synthetic project, and
  `common-configuration` needs the repository's build properties, which that project lacks. Nothing in
  this plugin needs accessors from it.
- The project uses `disableAutoTargetJvm()` because sibling conventions such as `test-inputs-check` are
  built for the running JDK rather than for Java 17.
