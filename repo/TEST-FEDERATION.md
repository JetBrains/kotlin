# Test Federation

The mono-repository is split into multiple 'Domains' (like 'Compiler', 'AnalysisApi', ...).
The CI can verify commits into such Domains independently.
'Plain old tests' of 'unaffected Domains' are not required for commits to prove correctness.
All tests, however, will be executed on master builds.

## Table of contents

- [What is a Domain? (Quick intuition)](#what-is-a-domain-quick-intuition)
- [Defining Domains](#defining-domains)
- [`^affects` commit command](#affects-commit-command)
  - [Domains fully affecting other Domains](#domains-fully-affecting-other-domains)
- [Local testing](#local-testing)
  - [Verifying domains](#verifying-domains)
  - [Updating the dump](#updating-the-dump)
  - [Checking domain dependencies](#checking-domain-dependencies)
- [Smoke Tests: Verifying commits on the federal level](#smoke-tests-verifying-commits-on-the-federal-level)
- [Running a small subset of tests, as smoke tests, automatically](#running-a-small-subset-of-tests-as-smoke-tests-automatically)
- [Contracts between Domains](#contracts-between-domains--single-tests--test-suites-affected-by-other-domains)
  - [Contracts require approval from the target team](#contracts-require-approval-from-the-target-team)
- [Nightly Tests](#nightly-tests)
- [TeamCity setup](#teamcity-setup)
    - [`Aggregate (master)` and `Aggregate (smoke)`](#aggregate-master-and-aggregate-smoke)
    - [`Domain (X)` configurations](#domain-x-configurations)
    - [`requiresAffectedDomain`: skipping entire build configurations](#requiresaffecteddomain-skipping-entire-build-configurations)

### What is a Domain? (Quick intuition)

A Domain is a **CI ownership and impact unit**, not an architecture concept.

- It answers: "If files in this area change, which tests should CI run?"
- It does **not** answer: "How should production code be layered or designed?"

Think of a Domain as a **change-radius boundary**:

- Inside the boundary: changes make this Domain affected.
- Outside the boundary: this Domain stays unaffected (unless connected via `fullyAffectedBy` or Contracts).

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
  fullyAffectedBy:
    - Compiler
```

Entries under `include` and `exclude` can be directory paths or glob patterns. A directory path matches the directory and
all its descendants, so the `Native` domain above includes everything under the `native` and `kotlin-native` directories.
When `include` and `exclude` entries overlap, the most specific matching entry takes precedence.
A domain is always marked as 'affected' if any file, belonging to the domain, is changed.

## '^affects' commit command

If a commit is known to affect another domain, the commit command `^affects:` can be used declare additional affected domains.

```
^affects: Gradle, AnalysisApi
^affects: Compiler

// Mark all domains as affected
^affects: *
```

### Domains fully affecting other Domains

Some domains might form a 'Domain/Subdomain' relationship, which can be expressed using 'fullyAffectedBy'.
A domain 'fullyAffectedBy' another domain will be marked as 'affected' by a set of changes if any of it's direct dependencies are
marked affected. In the example above:

A change which marks the 'larger Compiler domain' as affected will also mark the 'Native' domain as affected, while
a change isolated within the 'Native' domain will not affect the 'Compiler' domain.

Note: 'fullyAffectedBy' is **not** transitive. All dependencies have to be listed explicitly. 
This allows for some modules acting as 'API' boundaries.

### Local testing

#### Verifying domains

The declared domains will be 'expanded' into the actual files belonging to each domain. The dump file is verified on CI.
The file can be found at [domains.dump.txt](domains.dump.txt).

Verify it locally from the repository root with:
```shell
./gradlew :repo:codebase-tests:test --tests "org.jetbrains.kotlin.code.DomainsDumpTest" --rerun -Pkotlin.native.enabled=true
```

#### Updating the dump

Changes to `domains.yaml` might require an update of the dump file. Update it from the repository root with:

```shell
./gradlew :repo:codebase-tests:updateDomainsDump -Pkotlin.native.enabled=true
```

Alternatively, run `scripts/update-domains.sh` or use the `Update domains.dump.txt` run configuration in IntelliJ.
Use `Update all project dumps` to refresh all project dumps at once.

#### Checking domain dependencies

You can verify dependencies between domains by making a relevant change,
committing it locally and then invoking this command:
```shell
./gradlew -Ptest.federation.enabled=true inferAffectedDomains
```

If you want to check how some specific task would work when only specific domains were changed, you need to run 

```shell
./gradlew -Ptest.federation.enabled=true -Ptest.federation.mode=Smoke -Ptest.federation.affected.domains="XXX" :some:module:test
```

Available values for affected domains (`XXX`):
- some single domain (e.g. `-Ptest.federation.affected.domains=CompilerPlugins`)
- several affected domains (e.g. `-Ptest.federation.affected.domains=Wasm;Js`)
- all domains affected: `-Ptest.federation.affected.domains="*"`
- none domains affected: `-Ptest.federation.affected.domains="<none>"`

For other properties and their values you can check [runtimeEnvironment.kt](./test-federation-runtime/src/main/kotlin/org/jetbrains/kotlin/testFederation/runtimeEnvironment.kt).

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

A set of well-maintained contracts is always preferable to marking a domain as 'fullyAffectedBy' another domain,
as 'ContractTests' will enable actually building efficient pipelines for verifying commits, whereas 'fullyAffectedBy'
will require a full build of the affected domains.

```kotlin
@AffectedByJs
class MyImportantJsTests {
    // ...
}
```

Any commit to the `Js` domain will verify all contracts.

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

## TeamCity setup

> [!WARNING]
> This section describes the current TeamCity setup, which is not ideal. It's going to be simplified in the near future.

The Gradle-side properties described above (`test.federation.mode`, `test.federation.affected.domains`,
`test.federation.changed.domains`) are set by the CI. The TeamCity configurations that do this live in a separate repository,
[kotlin-infrastructure](https://jetbrains.team/p/kti/repositories/kotlin-infrastructure), under the `.teamcity` directory.

The mode a build was executed in is visible on the TeamCity build overview page (for example, `Mode: Full`), which is the quickest way to
check whether Test Federation skipped anything for a given run.

### `Aggregate (master)` and `Aggregate (smoke)`

There are two aggregate build configurations, and they behave differently depending on where they run:

| Aggregate            | On `master`                                              | On a branch (safe merge)                                                   |
|----------------------|----------------------------------------------------------|----------------------------------------------------------------------------|
| `Aggregate (master)` | everything runs in `Full` (affected domains = `*`)       | some builds run in `Full`, some in `Smoke`, depending on the changed files |
| `Aggregate (smoke)`  | everything runs in `Smoke` (affected domains = `<none>`) | does not run at all                                                        |

The only reason `Aggregate (smoke)` exists is to quickly see whether the smoke tests are green or red on `master`. It matters because
when smoke tests are red, **nobody can merge any change**.

Note: the two aggregates do **not** contain exactly the same set of build configurations. `Aggregate (smoke)` deliberately omits some
configurations and, for some expensive builds, includes dedicated 'smoke' variants instead of the full set of buckets (running all buckets in
smoke mode would mostly measure per-build overhead). This means the set of builds skipped in `Aggregate (master)` through
[`requiresAffectedDomain`](#requiresaffecteddomain-skipping-entire-build-configurations) and the set of builds filtered out of
`Aggregate (smoke)` are maintained separately and have to be kept in sync manually.

### `Domain (X)` configurations

The `Domain (X)` build configurations visible at the top level of the TeamCity project tree are **not** part of the quality gate. They only
run on `master` and exist to give an overview of which Domains are currently green and which are red.

### `requiresAffectedDomain`: skipping entire build configurations

Normally, no TeamCity-side configuration is needed for Test Federation: a build of an unaffected Domain is not skipped, it runs in `Smoke`
mode instead.

For builds where even the smoke run is not worth its cost, the TeamCity DSL offers `requiresAffectedDomain(Domain.X)`. During the safe merge
quality gate, if the requirement is not satisfied by the changed files, all steps of that build configuration are skipped and the build
effectively becomes a NOOP.

Note: `requiresAffectedDomain` is a stopgap. It is expected to be replaced by more dynamic build chains (a TeamCity feature) or a smart
trigger.
