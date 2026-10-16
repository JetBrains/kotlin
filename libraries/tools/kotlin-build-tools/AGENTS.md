# Kotlin Build Tools libraries

Gradle-independent libraries shared by Kotlin build tool integrations: the Kotlin Gradle plugin (KGP),
the Kotlin Maven plugin, JPS, kotlin-toolchain, Bazel rules and other integrations built on the
Build Tools API (BTA). [README.md](README.md) has the layout and the module list.

The convention plugin every library applies lives in `repo/gradle-build-conventions/kotlin-build-tools-library/`;
its README says what it sets up.

## Where code belongs

Put code here when it is build-tool logic that does not need Gradle:
- KGP logic that works on plain values (paths, strings, versions, metadata) and that another
  integration could reuse, or that is easier to test outside Gradle;
- operations built on BTA that several integrations would otherwise each implement;
- infrastructure every integration needs, such as downloading and caching a Kotlin/Native distribution.

Keep it out when it:
- takes or returns Gradle types (`Project`, `Task`, `Provider`, `Property`, `FileCollection`,
  `Configuration`), defines DSL, or wires tasks. That stays in KGP as a thin adapter;
- needs compiler internals. Expose them through BTA first (see [BTA AGENTS.md](../../../compiler/build-tools/AGENTS.md));
- serves a single integration with no reuse in sight. Keep it in that integration.

## Dependencies

Allowed:
- BTA: `api(project(":compiler:build-tools:kotlin-build-tools-api"))`. The only way to reach the compiler.
- Other Kotlin Build Tools libraries, with no cycles.
- Kotlin libraries such as kotlinx-coroutines, kotlinx-serialization and Ktor, at the versions in
  the version catalog (`libs`).

Never:
- Gradle (`gradleApi()`, `gradleKotlinDsl()`, `org.gradle:*`) or any `kotlin-gradle-plugin*` project.
  Nothing in the build enforces this yet, so check it in review. Move the Gradle-dependent part
  back to KGP instead.
- Compiler internals, Maven, JPS or IDE APIs.

Every dependency ships with every integration, and Bazel rules have to vendor it. Inside Gradle,
another plugin may already have loaded coroutines or serialization at a different version. So when
an implementation needs third-party dependencies, split API from implementation (see below) and
mention the new dependency in the commit message.

## Adding a library

1. `libraries/tools/kotlin-build-tools/<tool>/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kotlin-build-tools-library")
   }

   description = "Kotlin Build Tools <what the library does>"

   dependencies {
       api(project(":compiler:build-tools:kotlin-build-tools-api")) // only if the library uses BTA
   }
   ```
   `description` becomes the POM name. It must come after `plugins {}`.
2. `settings.gradle.kts`: add `include(":libraries:tools:kotlin-build-tools:<tool>")` next to the
   `abi-validation` includes. The directory name is the project name and the artifact name.
3. Root `build.gradle.kts`: add `dependsOn(":libraries:tools:kotlin-build-tools:<tool>:check")` to `toolsTest`,
   then run `./gradlew :repo:codebase-tests:updateTestLifecycleTaskDump -Pkotlin.native.enabled=true`.
4. Publish locally and add the expected POM at
   `repo/artifacts-tests/src/test/resources/org/jetbrains/kotlin/build-tools/<tool>/<tool>.pom`.
   For the first library, also add
   `/repo/artifacts-tests/src/test/resources/org/jetbrains/kotlin/build-tools/ "Kotlin Build Tools"`
   to `.space/CODEOWNERS` in the same commit: `SpaceCodeOwnersTest` rejects a rule that matches no file.
5. Run `./gradlew :libraries:tools:kotlin-build-tools:<tool>:apiDump` and commit `api/<tool>.api`.
6. Add tests under `src/test/kotlin` (see [Testing](#testing)).
7. Create `<tool>/code-rules.md` with the single line `@../library-rules.md`. That enables the AI review
   rules in [library-rules.md](library-rules.md) for the library (`/review` on a PR, or
   `./gradlew -q reviewCode` locally; see `repo/auto-code-review/README.md`), then run
   `./gradlew :repo:auto-code-review:test --tests "org.jetbrains.kotlin.code.review.RepoCodeRulesTests" -q`.
8. Add the library to the table in [README.md](README.md).

## Testing

```bash
# One library: compile, unit tests, API dump check
./gradlew :libraries:tools:kotlin-build-tools:<tool>:check -q

# A single test class
./gradlew :libraries:tools:kotlin-build-tools:<tool>:test --tests "org.jetbrains.kotlin.buildtools.<tool>.<Subject>Test" -q

# After changing public declarations
./gradlew :libraries:tools:kotlin-build-tools:<tool>:apiDump

# Published POM after a dependency change; this test compares only locally published POMs
./gradlew :libraries:tools:kotlin-build-tools:<tool>:publish -q
./gradlew :repo:artifacts-tests:test --tests "org.jetbrains.kotlin.code.MavenMetadataTest.generateMavenMetadataTests" -q
```

CI runs every library through the `toolsTest` lifecycle task; a library missing from it (step 3 above)
is not tested in CI.

Every library has unit tests in its own module. A change that adds or alters behaviour without
tests is incomplete. The only exception is a module that holds nothing but declarations.

- Tests live under `src/test/kotlin`, in the package of the code under test, in a class named `<Subject>Test`.
  JUnit 5 and `kotlin-test` come from the convention plugin.
- Cover every public entry point: the main path, edge cases (empty input, missing or duplicate files,
  paths with spaces or non-ASCII characters) and failure paths, asserting the exception type and message.
- Use `@TempDir` for files. The convention applies `test-inputs-check`, which fails a test that reads
  files not declared as Gradle inputs; the repository layout, the user's home and the network are off limits.
- Test against small fakes of BTA interfaces. Real compilation tests are few and load
  `kotlin-build-tools-impl` in an isolated class loader, as `compiler/build-tools/kotlin-build-tools-api-tests` does.
- No Gradle in tests: no TestKit, no `ProjectBuilder`. A test that needs Gradle belongs in KGP.
- No shared mutable state between tests, no ordering assumptions, no sleeps.
- A bug fix comes with a test that fails without it.

## Rules

- `explicitApi()` is on, and every public declaration has KDoc.
- The public API is dumped into `api/<tool>.api` by binary-compatibility-validator. Commit the dump with
  the change that alters it; `check` fails on a stale dump. Review the dump diff like code: it is the
  record of what consumers see.
- Plain Kotlin/JDK types at the boundary: `java.nio.file.Path`, `String`, collections, data or value
  classes. No lazy Gradle-style wrappers.
- Take collaborators as parameters (a BTA `KotlinLogger` for logging, for example), not from global state.
- Throw specific exceptions with actionable messages; the integration decides how to surface them.
- One library, one package: `org.jetbrains.kotlin.buildtools.<tool>`.
- Don't override the Kotlin language/API version, JVM target or stdlib setup per module. The
  convention plugin matches KGP's, and the code runs inside KGP on the oldest supported Gradle.
- Create a shared module only once two libraries need the same code.

## Extracting code from KGP

1. Find the Gradle-free core. Inputs that are Gradle types get resolved in KGP and passed in as plain values.
2. Create the library (checklist above) and move the core there with its unit tests. Add tests for
   untested behaviour before the move, so the move itself shows no behaviour change. Refactorings go
   in separate commits.
3. In KGP, depend on the library from the source set that uses it, usually
   `commonImplementation(project(":libraries:tools:kotlin-build-tools:<tool>"))` in
   `libraries/tools/kotlin-gradle-plugin/build.gradle.kts`.
4. Add the library to `kotlinGradlePluginAndItsRequired` in
   `repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/tasks.kt`, so KGP integration tests install it.
5. Keep the KGP functional and integration tests that cover the Gradle wiring, and run them
   (see [KGP AGENTS.md](../kotlin-gradle-plugin/AGENTS.md)).

## API and implementation split

A library is a single module by default. Split it when the implementation has third-party
dependencies or must be loaded in an isolated class loader: the API module
(`<tool>/<tool>-api/`, project `:libraries:tools:kotlin-build-tools:<tool>:<tool>-api`, artifact `<tool>-api`)
holds interfaces, data classes and a loader entry point and has no third-party dependencies; the
implementation stays in `<tool>/` and depends on it. Both apply the convention plugin. The directory is
`<tool>-api`, not `api`, because `api/` is where the enclosing project's API dump lives.

## Changing the convention plugin

The plugin is a precompiled script in `repo/gradle-build-conventions/kotlin-build-tools-library/`, its own
project in the conventions build (not part of `buildsrc-compat`, which is a buildSrc migration meant to
shrink). The conventions build compiles with warnings as errors.

- A plugin it applies by id must be a dependency of that project's `build.gradle.kts`; that is where
  `binary-compatibility-validator` and `test-inputs-check` come from.
- `common-configuration` stays applied from the script body (`apply(plugin = ...)`), not from `plugins {}`:
  accessor generation would apply it to a synthetic project without the repository's build properties.
- Keep it lazy: providers and `tasks.register`, no `afterEvaluate`, nothing that touches other projects.
  Per-library configuration is the `description` only; add a typed extension once a second knob is needed.
- Verify a change with `./gradlew -p repo/gradle-build-conventions :kotlin-build-tools-library:compileKotlin -q`
  and then `check` and `publish` of a library (or a throwaway module, if none exists yet), and inspect the
  published POM and the API dump. Update the plugin's README when its behaviour changes.

## Commits

`[Build]` for build and convention changes. For library code, the tag of the area the library serves,
for example `[Gradle]` when extracting from KGP. See [commit-guidelines.md](../../../.ai/commit-guidelines.md).
