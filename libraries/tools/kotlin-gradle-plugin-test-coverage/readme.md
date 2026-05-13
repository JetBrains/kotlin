# Test coverage collection in Kotlin Build Tools

To collect test coverage in Kotlin Build Tools, we use [JaCoCo](https://www.jacoco.org/jacoco/).
For functional/unit tests the JaCoCo Gradle plugin is used and instrumentation is performed on the fly.
For integration tests we replace the `kotlin-gradle-plugin` JAR file in mavenLocal with its
instrumented version and run tests against it, passing the JaCoCo agent to the Gradle TestKit.

Reports are aggregated using Gradle's
[`jacoco-report-aggregation`](https://docs.gradle.org/current/userguide/jacoco_report_aggregation_plugin.html)
plugin: the producer projects (`:kotlin-gradle-plugin`, `:kotlin-gradle-plugin-api`,
`:kotlin-gradle-plugin-integration-tests`) expose coverage data, class dirs, and source dirs via
outgoing configurations; the aggregator consumes them through `jacocoAggregation(project(...))`
dependencies. No project reads another project's build directory directly, so this is compatible
with Gradle Project Isolation and respects normal task-dependency and up-to-date semantics.

## How to collect coverage

Test coverage collection is disabled by default. To run tests with coverage, pass
`kgp.jacoco.enabled=true`:

```bash
./gradlew :kotlin-gradle-plugin:functionalTest -Pkgp.jacoco.enabled=true
```

or

```bash
./gradlew :kotlin-gradle-plugin-integration-tests:kgpAllParallelTests -Pkgp.jacoco.enabled=true
```

Coverage data is written to `build/jacoco/` in each producing project.

> Note: when `kgp.jacoco.enabled=true`, the test tasks are configured with `ignoreFailures = true`,
> so failing tests do not abort the build. This lets the dependent coverage report run on the
> partial `.exec` data. The failures are still printed in the test output.

## How to open reports locally

You can open generated HTML reports with IntelliJ IDEA — double-click `index.html`. IDEA shows
coverage for all loaded modules in the Coverage Tool Window and renders per-file coverage percent
in the Project view. (Note: IDEA refuses to open reports located outside the project dir.)

## How to generate reports

Each report task auto-triggers the relevant tests via Gradle's dependency graph — running the
report alone is enough:

```bash
./gradlew :kotlin-gradle-plugin-test-coverage:functionalCoverageReport -Pkgp.jacoco.enabled=true
# triggers :kotlin-gradle-plugin:functionalTest

./gradlew :kotlin-gradle-plugin-test-coverage:integrationCoverageReport -Pkgp.jacoco.enabled=true
# triggers :kotlin-gradle-plugin-integration-tests:kgpAllParallelTests

./gradlew :kotlin-gradle-plugin-test-coverage:combinedCoverageReport -Pkgp.jacoco.enabled=true
# merges both
```

To collect coverage for a narrower subset of integration tests, invoke the desired test task
explicitly and then the report:

```bash
./gradlew :kotlin-gradle-plugin-integration-tests:kgpJvmTests \
          :kotlin-gradle-plugin-test-coverage:integrationCoverageReport \
          -Pkgp.jacoco.enabled=true
```

Each task produces both HTML and XML output under `build/reports/jacoco/<reportName>/`. If no
`.exec` data exists for the requested test suite, the task is skipped cleanly.
