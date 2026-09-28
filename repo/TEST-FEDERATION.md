# Test Federation

Test Federation selects the tests that must run and pass before a commit can be merged to master.
It uses changed files, domain declarations, test annotations, and the `^test:` commit command.
All tests run in master builds, even if they were not required for merging the commit to master.

## Table of contents

- [Domains and changed domains](#domains-and-changed-domains)
- [Which tests are required for merging to master?](#which-tests-are-required-for-merging-to-master)
- [Defining domains](#defining-domains)
    - [Running all tests on changes in another domain](#running-all-tests-on-changes-in-another-domain)
- [Running individual tests on changes in another domain](#running-individual-tests-on-changes-in-another-domain)
- [Always running tests](#always-running-tests)
- [`^test:` commit command](#test-commit-command)
- [Nightly tests](#nightly-tests)
- [Running a subset of a test task](#running-a-subset-of-a-test-task)
- [Local testing](#local-testing)
    - [Checking which files belong to each domain](#checking-which-files-belong-to-each-domain)
    - [Updating the dump](#updating-the-dump)
    - [Running tests for specified changed domains](#running-tests-for-specified-changed-domains)
- [Extra: Domains and code structure](#extra-domains-and-code-structure)
- [Extra: Contract tests](#extra-contract-tests)
- [Extra: Smoke tests](#extra-smoke-tests)

## Domains and changed domains

A domain is a named group of files, defined in [domains.yaml](./domains.yaml). For example, `Js` includes the files under `js`.
A domain is **changed** when at least one file belonging to it is changed.
A Domain is a **CI ownership and impact unit**, not an architecture concept.

A test task belongs to the domains of its project directory. When a domain is changed, all its tests must run and pass
before the commit can be merged to master. Other test filters, such as `@NightlyTest`, still apply.

## Which tests are required for merging to master?

The following tests must run and pass before a commit can be merged to master:

- All tests in changed domains.
- All tests in a domain that lists a changed domain in `mustRunAllTestsOnChangesIn`.
- Individual tests annotated with `@MustRunOnChangesInXYZ` when domain `XYZ` is changed.
- Tests annotated with `@MustRunAlways`, regardless of which domains are changed.
- All tests in domains listed in the `^test:` commit command.
- Any additional tests selected by the test task's `smokeTestConfig`.

Other test filters still apply. In particular, `@NightlyTest` tests are not required for merging to master.

Running all tests in a domain does **not** make that domain changed. Only changed files make a domain changed.
Neither `mustRunAllTestsOnChangesIn` nor `^test:` triggers `@MustRunOnChangesInXYZ` tests in other domains unless `XYZ` itself is changed.

## Defining domains

Use `include` and `exclude` in [domains.yaml](./domains.yaml) to specify which files belong to a domain. For example:

```yaml
Js:
  include:
    - "js"
    - "compiler/ir/backend.js"
  mustRunAllTestsOnChangesIn:
    - CoreLibs
```

Entries under `include` and `exclude` can be directory paths or glob patterns. A directory path matches the directory and all its
descendants. When entries overlap, the most specific matching entry takes precedence.
A domain is changed when any file belonging to it is changed.

### Running all tests on changes in another domain

Use `mustRunAllTestsOnChangesIn` to require all tests in this domain to run and pass when a listed domain is changed.
In the example above, all `Js` tests are required for merging to master when `CoreLibs` is changed.
This declaration does not require all `CoreLibs` tests to run when only `Js` is changed.
List every domain whose changes should require these tests explicitly: the declaration is **not transitive**.

## Running individual tests on changes in another domain

Use `@MustRunOnChangesInXYZ` to require a test to run and pass when domain `XYZ` is changed.
For example, `@MustRunOnChangesInJs` requires the annotated tests for merging to master when `Js` is changed,
even if the tests belong to another domain.

```kotlin
@MustRunOnChangesInJs
class MyImportantJsTests {
    // ...
}
```

The annotation can be placed on a test method, a test class, an abstract test class, or another annotation.
Other test filters, such as `@NightlyTest`, still apply.

This annotation adds a reason to run a test. The test still runs whenever all tests in its own domain must run.

Adding or changing these annotations requires approval from both the team owning the tests and the team owning the named domain.
See [Extra: Contract tests](#extra-contract-tests) for why these tests are useful.

## Always running tests

Use `@MustRunAlways` to require a test to run and pass regardless of which domains are changed.
These tests are required for merging to master. Other test filters, such as `@NightlyTest`, still apply.
With JUnit 5 (or higher), the annotation can be placed:

- on the test method directly

```kotlin
@MustRunAlways
@Test
fun `my important test`() {
    // ...
}
```

- on the test class

```kotlin
@MustRunAlways
class MyImportantTest {
    @Test
    fun `my important test`() {
        // ...
    }
}
```

- on any abstract test class

```kotlin
@MustRunAlways
abstract class AbstractImportantTests {
    // ...
}
```

- as a meta-annotation on another annotation

```kotlin
@MustRunAlways
annotation class MyImportantTest

@MyImportantTest
@Test
fun `my important test`() {
    // ...
}
```

Tests annotated with `@MustRunAlways` must be fast and stable, because they run for unrelated changes too.

## `^test:` commit command

Add `^test:` to a commit message to require all tests in the listed domains to run and pass before merging to master.
Use `^test: *` to require all tests in all domains. Other test filters, such as `@NightlyTest`, still apply.

```
^test: Gradle, AnalysisApi
^test: Frontend
```

To require all tests in all domains:

```
^test: *
```

The command does not make the listed domains changed. It does not trigger `mustRunAllTestsOnChangesIn` declarations or
`@MustRunOnChangesInXYZ` tests in other domains.

## Nightly tests

Use `@NightlyTest` for tests that run only when nightly tests are enabled. These tests are not required for merging to master.
The annotation can be placed on a test method or a test class.

Nightly tests are enabled by default in local Gradle runs. Use `-Pnightly=false` to disable them locally.

```kotlin
class MyTests {
    @NightlyTest
    @Test
    fun `my long nightly test`() {
        superLongOperation()
    }

    @Test
    fun `my regular test`() {
        // ...
    }
}
```

When all tests in this class are selected, `my regular test` runs before merging to master. `my long nightly test` runs only when
nightly tests are enabled. `@MustRunAlways` and `@MustRunOnChangesInXYZ` do not override this restriction.

Long-running tests or tests that have not yet proven stable can be marked with `@NightlyTest` if they are not required for merging to master.

## Running a subset of a test task

A test task runs in one of two modes:

- `Full`: run all tests in the task.
- `Smoke`: run tests annotated with `@MustRunAlways`, tests annotated with `@MustRunOnChangesInXYZ` for a changed domain `XYZ`,
  and any additional tests selected by `smokeTestConfig`.

Other test filters, such as `@NightlyTest`, still apply in both modes.
Test Federation uses `Full` when all tests in the task's domain must run, and `Smoke` otherwise.
Setting `smokeTestConfig = SmokeTestConfig.Disabled` skips the task in `Smoke` mode, including its annotated tests.

Use `smokeTestConfig` to select additional tests when the task runs in `Smoke` mode. By default, no additional tests are selected.
For example, this configuration selects roughly 5% of the tests in addition to the annotated tests:

```kotlin
tasks.withType<Test>().configureEach {
    smokeTestConfig = SmokeTestConfig.Enabled(
        autoSmokeTestPercentage = 5
    )
}
```

The selection is stable: it uses the fully qualified name and unique ID of each test.
Choose fast and stable tests, because the selected tests are required for merging to master even for unrelated changes.

To require all tests in a task regardless of which domains are changed:

```kotlin
tasks.withType<Test>().configureEach {
    smokeTestConfig = SmokeTestConfig.RunAllTests
}
```

Other test filters still apply.

## Local testing

### Checking which files belong to each domain

[domains.dump.txt](domains.dump.txt) lists the files belonging to each domain. CI checks that it matches the domain declarations.
Run the same check locally from the repository root:

```shell
./gradlew :repo:codebase-tests:test --tests "org.jetbrains.kotlin.code.DomainsDumpTest" --rerun -Pkotlin.native.enabled=true
```

### Updating the dump

Changes to `domains.yaml` might require an update of the dump file. Update it from the repository root with:

```shell
./gradlew :repo:codebase-tests:updateDomainsDump -Pkotlin.native.enabled=true
```

Alternatively, run `scripts/update-domains.sh` or use the `Update domains.dump.txt` run configuration in IntelliJ.
Use `Update all project dumps` to refresh all project dumps at once.

### Running tests for specified changed domains

To run a test task in `Smoke` mode as if `Js` were changed:

```shell
./gradlew :some:module:test \
  -Ptest.federation.enabled=true \
  -Ptest.federation.mode=Smoke \
  -Ptest.federation.changed.domains="Js"
```

This runs `@MustRunAlways` tests, `@MustRunOnChangesInJs` tests, and any additional tests selected by `smokeTestConfig`.
Use `-Ptest.federation.mode=Full` to run all tests in the task. Other test filters still apply.

`test.federation.changed.domains` accepts:

- a single domain (for example, `CompilerPlugins`)
- several domains separated by semicolons (for example, `Wasm;Js`)
- all domains (`*`)
- no domains (`<none>`)

For other properties and their values, see
[runtimeEnvironment.kt](test-runtime/src/main/kotlin/org/jetbrains/kotlin/test/federation/runtimeEnvironment.kt).

## Extra: Domains and code structure

Domains group files for selecting tests, not for prescribing code structure. A subsystem can span several domains, and a domain can
include files from several locations. Explicit, non-transitive test declarations let teams decide which tests are required for merging to master
without requiring every test in every downstream domain.

## Extra: Contract tests

A contract test checks behavior that one domain relies on in another domain. `@MustRunOnChangesInXYZ` can be used to require such a test
when the other domain is changed. These annotations use JUnit tags of the form `contract:XYZ`.

A small set of these tests is preferable to `mustRunAllTestsOnChangesIn` when it covers the required behavior: fewer tests are required for
merging to master. Both teams must approve changes to these tests because the tests describe behavior that they agree to preserve.

## Extra: Smoke tests

A smoke test is a quick check of core functionality. `@MustRunAlways` can be used for smoke tests that should run for every change,
but not every smoke test needs to run for unrelated changes. Selecting a percentage of fast, stable tests through `smokeTestConfig` is
another way to check core functionality for unrelated changes.
