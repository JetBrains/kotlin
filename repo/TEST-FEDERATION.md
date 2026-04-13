# Test Federation

The mono-repository is split into multiple 'Domains' (like 'Compiler', 'AnalysisApi', ...).
The CI can verify commits into such Domains independently.
'Plain old tests' of 'unaffected Domains' are not required to be executed on CI.

## Defining Domains

Domains are defined in the [domains.yaml](./domains.yaml) file
e.g. the `Native` domain could be defined as:

```yaml
Native:
  home: "kotlin-native"
  include:
    - "native/**"
    - "kotlin-native/**"
  fullyAffectedBy:
    - Compiler
```

where it gets assigned the 'kotlin-native' directory as its home.
Files belonging to this 'Native' domain are included using the `native/**` and `kotlin-native/**` globs.
A domain is always marked as 'affected' if any file, belonging to the domain, is changed.

### Domains fully affecting other Domains

Some domains might form a 'Domain/Subdomain' relationship which can be expressed using 'fullyAffectedBy.'
A domain, which is fully affected by another domain, will always be marked as 'affected' by a set of changes if any of the
(transitive) dependencies are marked affected. In the example above:

A change which marks the 'larger Compiler domain' as affected will also mark the 'Native' domain as affected, while
a change isolated within the 'Native' domain will not affect the 'Compiler' domain.

### Verifying the declaration: [domain.dump.txt](./domain.dump.txt)

The declared domains will be 'expanded' into the actual files belonging to each domain. The dump file will be verified on CI.

#### Verifying domains or updating the dump

```shell
./gradlew :gradle-build-conventions:test-federation-convention:test --tests "org.jetbrains.kotlin.testFederation.DomainsDumpTest" --rerun
```

#### Updating the dump
Changes to the domain.yaml file might require an update of the dump file.
This can be done by executing the 'update-domains' script:

```shell
cd ..
./scripts/update-domains.sh
```

### Smoke Tests: Verifying commits on the federal level

All tests of affected 'Domains' will be executed on CI. Running tests of a domain, which is not affected can be done by
marking a test as 'SmokeTest'. Using junit5 (or higher) allows using the `@SmokeTest` annotation

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

As unstable/flaky smoke tests affect the entire team, fixing them is a high priority.

### Entire test tasks as 'smoke tests'

It is possible to check in an entire test task as 'smoke test'.
This can be done by using the `isSmokeTest` extension property on the `Test` task.
E.g.

```kotlin
tasks.withType<Test>().configureEach {
    isSmokeTest = true
}
```

### Contracts between Domains | Single Tests / Test Suites affected by other domains

Some Domains might rely on the behavior or API of another Domain.
Such requirements can be expressed as 'Contract' between two Domains.
Any test can be promoted to a 'Contract Test' using the relevant `@AffectedByXYZ` annotation.
e.g., a test that defines a contract to the 'Js' compiler might be marked as `@AffectedByJs`.

A set of well-maintained contracts is always more preferable than marking a domain as 'fullyAffectedBy' another domain, 
as 'ContractTests' will enable actually building efficient pipelines for verifying commits, whereas 'fullyAffectedBy'
will require a full build of the affected domains.

```kotlin
@AffectedByJs
class MyImportantJsTests {
    // ...
}
```

any commit to the `Js` domain will verify all contracts.

##### Contracts require approval from the target team
Declaring a contract is transactional between at least two teams (owning their domains). Defining and changing a contract requires
the explicit approval of both teams. 
