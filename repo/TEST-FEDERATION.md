# Test Federation

The mono-repository is split into multiple 'Domains' (like 'Compiler', 'AnalysisApi', ...).
The CI can verify commits into such Domains independently.
'Plain old tests' of 'unaffected Domains' are not required for commits to prove correctness.
All tests, however, will be executed on master builds.

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
    - "native/**"
    - "kotlin-native/**"
  fullyAffectedBy:
    - Compiler
```

Files belonging to this 'Native' domain are included using the `native/**` and `kotlin-native/**` globs.
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

### Verifying the declaration: [domains.dump.txt](./domains.dump.txt)

The declared domains will be 'expanded' into the actual files belonging to each domain. The dump file will be verified on CI.

#### Verifying domains or updating the dump

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
