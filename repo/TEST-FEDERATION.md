# Test Federation

The mono-repository is split into multiple 'Domains' (like 'Compiler', 'AnalysisApi', ...).
The CI can verify commits into such Domains independently.
'Plain old tests' of 'unaffected Domains' are not required for commits to prove correctness.
All tests, however, will be executed on master builds.

## Table of contents

- [What is a Domain? (Quick intuition)](#what-is-a-domain-quick-intuition)
- [Defining Domains](#defining-domains)
- [`^affects` commit command](#affects-commit-command)
  - [Full-domain contracts](#full-domain-contracts)
- [Local testing](#local-testing)
  - [Verifying domains](#verifying-domains)
  - [Updating the dump](#updating-the-dump)
  - [Checking domain dependencies](#checking-domain-dependencies)
- [Smoke Tests: Verifying commits on the federal level](#smoke-tests-verifying-commits-on-the-federal-level)
- [Running a small subset of tests, as smoke tests, automatically](#running-a-small-subset-of-tests-as-smoke-tests-automatically)
- [Contracts between Domains](#contracts-between-domains--single-tests--test-suites-affected-by-other-domains)
  - [Contracts require approval from the target team](#contracts-require-approval-from-the-target-team)
- [Nightly Tests](#nightly-tests)

### What is a Domain? (Quick intuition)

A Domain is a **CI ownership and impact unit**, not an architecture concept.

- It answers: "If files in this area change, which tests should CI run?"
- It does **not** answer: "How should production code be layered or designed?"

Think of a Domain as a **change-radius boundary**:

- Inside the boundary: changes make this Domain affected.
- Outside the boundary: this Domain stays unaffected (unless connected by a contract).

In other words, Domains model **test impact**, not **code structure purity**. A single subsystem can span multiple Domains,
and one Domain can include files from multiple places if that gives better CI behavior.

## Defining Domains

Domains are defined in the [domains.yaml](./domains.yaml) file.
e.g., the `Native` domain could be defined as:

```yaml
Native:
  include:
    - "native"
    - "kotlin-native"
  contract:
    - Compiler
```

Entries under `include` and `exclude` can be directory paths or glob patterns. A directory path matches the directory and
all its descendants, so the `Native` domain above includes everything under the `native` and `kotlin-native` directories.
When `include` and `exclude` entries overlap, the most specific matching entry takes precedence.
A domain is always marked as 'directly affected' if any file belonging to the domain is changed.

## '^affects' commit command

If a commit is known to affect another domain, the commit command `^affects:` can be used declare additional affected domains.

```
^affects: Gradle, AnalysisApi
^affects: Compiler

// Mark all domains as affected
^affects: *
```

### Full-domain contracts

Some domains require their complete test suite to run when another domain changes. This relationship is declared as a
full-domain `contract`. In the example above:

A change that affects `Compiler` runs the `Native` domain in full mode, while a change isolated within `Native` does not
affect `Compiler`. The authoritative affected-domain set itself is not expanded by contracts.

Full-domain contracts are **not** transitive. All contracted domains have to be listed explicitly. This allows some
modules to act as API boundaries.

### Local testing

#### Verifying domains

The declared domains will be 'expanded' into the actual files belonging to each domain. The dump file will be verified on CI.
The file can be found here [domains.dump.txt](domains.dump.txt).

Locally, it can be ran us:
```shell
./gradlew :gradle-build-conventions:test-federation-convention:test --tests "org.jetbrains.kotlin.testFederation.DomainsDumpTest" --rerun
```

#### Updating the dump

Changes to the domains.yaml file might require an update of the dump file.
This can be done by executing the 'update-domains' script:

```shell
cd ..
./scripts/update-domains.sh
```

#### Checking domain dependencies

You can verify dependencies between domains by making a relevant change,
committing it locally and then invoking this command:
```shell
./gradlew -Ptest.federation.enabled=true inferAffectedDomains
```

To check how a specific task behaves for a given set of changed domains, set the affected domains:

```shell
./gradlew :some:module:test \
  -Ptest.federation.enabled=true \
  -Ptest.federation.mode=Smoke \
  -Ptest.federation.affected.domains="Js;Wasm"
```

`test.federation.affected.domains` controls which contracts run in smoke mode and which domains run in full mode. A
full-domain contract can make additional domains run in full mode without adding them to the affected-domain set. The
property accepts:

- a single domain (for example, `CompilerPlugins`)
- several domains separated by semicolons (for example, `Wasm;Js`)
- all domains (`*`)
- no domains (`<none>`)

For other properties and their values, see
[runtimeEnvironment.kt](./test-federation-runtime/src/main/kotlin/org/jetbrains/kotlin/testFederation/runtimeEnvironment.kt).

### Smoke Tests: Verifying commits on the federal level

All tests of affected 'Domains' will be executed on CI. Running tests of a domain that is not affected can be done by
marking a test as a 'SmokeTest'. Using JUnit 5 (or higher) allows using the `@SmokeTest` annotation.

- on the test method directly

```kotlin
@SmokeTest
@Test
fun `my important test`() {
    // ...
}
```

- on the test class

```kotlin
@SmokeTest
class MyImportantTest {
    @Test
    fun `my important test`() {
        // ...
    }
}
```

- on any abstract test class

```kotlin
@SmokeTest
abstract class AbstractImportantTests {
    // ...
}
```

- as a meta-annotation on another annotation

```kotlin
@SmokeTest
annotation class MyImportantTest

@MyImportantTest
fun `my important test`() {
    // ...
}
```

Smoke tests are always executed on CI, no matter the affected domains.
Checking in a smoke test requires the test to fulfill the following criteria:

- The test is very stable
- The test is fast

Because unstable/flaky smoke tests affect the entire team, fixing them is a high priority.

### Running a small subset of tests, as smoke tests, automatically

Some test tasks do not have a clear candidate that stands out as a 'Smoke Test'. However, if all tests are quick and stable,
running a percentage of those tests in 'smoke test mode' might be a good strategy for gaining confidence when testing unrelated
changes. Any test task, therefore, allows specifying a 'smokeTestConfig'.

Example: Run 5% of all tests in 'Smoke Test Mode'.
When a commit is verified on CI, but the domain to which this test belongs is 'unaffected', then roughly 5% of the defined
tests will still execute.

Note: The selected tests are stable as the selection is based upon the FQN and unique ID of the test.

```kotlin
tasks.withType<Test>().configureEach {
    smokeTestConfig = SmokeTestConfig.Enabled(
        autoSmokeTestPercentage = 5
    )
}
```

Sometimes an entire test task should *always* run, even in 'smoke test mode'.

```kotlin
tasks.withType<Test>().configureEach {
    smokeTestConfig = SmokeTestConfig.RunAllTests
}
```

This will ensure that the test task is always executed and all tests are verified.

### Contracts between Domains | Single Tests / Test Suites affected by other domains

Some Domains might rely on the behavior or API of another Domain.
Such requirements can be expressed as a 'Contract' between two Domains.
Any test can be promoted to a 'Contract Test' using the relevant `@AffectedByXYZ` annotation.
e.g., a test that defines a contract to the 'Js' compiler might be marked as `@AffectedByJs`.

A focused test contract is preferable to a full-domain contract because it enables efficient verification pipelines,
whereas a full-domain contract requires a full build of the declaring domain.

```kotlin
@AffectedByJs
class MyImportantJsTests {
    // ...
}
```

Any commit that affects the `Js` domain will verify all contracts associated with `Js`. This includes domains listed
explicitly using `^affects`. A domain whose full-domain contract is triggered runs its full test suites, but is not added
to the affected-domain set and therefore does not activate contracts associated with that domain in smoke-mode tasks.

##### Contracts require approval from the target team

Declaring a contract is transactional between at least two teams (owning their domains). Defining and changing a contract requires
the explicit approval of both teams.

### Nightly Tests

Some tests, test-classes or even entire suites of tests might not qualify for our 'master aggregate'.
Typically, nightly tests are 'long' or have not proven their stability (yet), while not being 'necessary' as 'mater quality gate'.
Marking a test as 'nighlty' is done by using the `@NightlyTest` annotation

```kotlin
class MyTests {
    @NightlyTest
    @Test
    fun `my looong nightly test`() {
        superLongOperation()
    }

    @Test
    fun `my regular test`() {

    }
}
```

The above example will only execute 'my regular test' during safe-merge, while the `my looong nightly test` is only executed nightly.
