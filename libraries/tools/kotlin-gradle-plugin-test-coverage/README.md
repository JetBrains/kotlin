# Test coverage collection in Kotlin Build Tools

To collect test coverage in Kotlin Build Tools, we use [JaCoCo](https://www.jacoco.org/jacoco/). To measure coverage we replace the
`kotlin-gradle-plugin` and `kotlin-gradle-plugin-api` JAR file with its instrumented version and run tests against it. For integration
tests the JaCoCo agent is passed to the Gradle TestKit build JVMs; for functional tests the standard Gradle `jacoco` plugin attaches the
agent to the test JVM, and the pre-instrumented classes report their coverage through it.

Reports are aggregated using Gradle's
[`jacoco-report-aggregation`](https://docs.gradle.org/current/userguide/jacoco_report_aggregation_plugin.html)
plugin: the producer projects (`:kotlin-gradle-plugin`, `:kotlin-gradle-plugin-api`,
`:kotlin-gradle-plugin-integration-tests`) expose coverage data, class dirs, and source dirs via outgoing configurations; the aggregator
consumes them through `jacocoAggregation(project(...))`
dependencies. No project reads another project's build directory directly, so this is compatible with Gradle Project Isolation and respects
normal task-dependency and up-to-date semantics.

In `:kotlin-gradle-plugin` the coverage-data variant (`coverageDataElementsForFunctionalTest`) is created automatically by the `jacoco` +
`jvm-test-suite` plugin combination, since `functionalTest` is a `JvmTestSuite` there. `:kotlin-gradle-plugin-integration-tests` cannot
model its tests as a single suite (one umbrella task spans several `Test` tasks sharing an exec file), so it registers an equivalent
variant manually via `registerKgpTestCoverageDataVariant`.

## How to collect coverage

Test coverage collection is disabled by default. The reasons are performance and unnecessary instrumentation that could affect some tests. 
To run tests with coverage, pass `kgp.jacoco.enabled=true`:

```bash
./gradlew :kotlin-gradle-plugin:functionalTest -Pkgp.jacoco.enabled=true
```

or

```bash
./gradlew :kotlin-gradle-plugin-integration-tests:kgpAllParallelTests -Pkgp.jacoco.enabled=true
```

Coverage data is written to `build/jacoco/` in each project with tests.

> Note: when `kgp.jacoco.enabled=true`, the test tasks are configured with `ignoreFailures = true`,
> so failing tests do not abort the build. This lets the dependent coverage report run on the
> partial `.exec` data. The failures are still printed in the test output.

## How to open coverage data locally

You can open generated `.exec` coverage data with IntelliJ IDEA. It requires plugin *Code Coverage for Java*. IDEA shows coverage for all loaded modules in the Coverage Tool Window and
renders per-file coverage percent in the Project view.

## How to generate HTML/XML reports

By default each report task auto-triggers the relevant tests via Gradle's dependency graph — running the report alone is enough:

```bash
./gradlew :kotlin-gradle-plugin-test-coverage:functionalCoverageReport -Pkgp.jacoco.enabled=true
# triggers :kotlin-gradle-plugin:functionalTest

./gradlew :kotlin-gradle-plugin-test-coverage:integrationCoverageReport -Pkgp.jacoco.enabled=true
# triggers :kotlin-gradle-plugin-integration-tests:kgpAllParallelTests

./gradlew :kotlin-gradle-plugin-test-coverage:combinedCoverageReport -Pkgp.jacoco.enabled=true
# merges both
```
