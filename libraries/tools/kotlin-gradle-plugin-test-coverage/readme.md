# Test coverage collection in Kotlin Build Tools

To collect test coverage in Kotlin Build Tools, we use [JaCoCo](https://www.jacoco.org/jacoco/)
Fun functional/unit tests Jacoco Gradle plugin used and instrumentation performed on the fly.
For integration tests we replace the kotlin-gradle-plugin jar file in mavenLocal with its instrumented version and run tests again it,
passing
jacoco agent to the Gradle TestKit.

## How to collect coverage

By default, the test coverage collection is disabled. To run tests with coverage pass `kgp.jacoco.enabled=true` flag to test the execution
task

```bash
./gradlew :kotlin-gradle-plugin:functionalTest -Pkgp.jacoco.enabled=true
```

ir

```bash
./gradlew :kotlin-gradle-plugin-integration-tests:kgpAllParallelTests -Pkgp.jacoco.enabled=true
```

They will dump coverage data to `build/jacoco` directory.

## How to open reports locally

You can open generated reports with IntelliJ IDEA by just a double click. For some reason it will not open a report located outside of the
project dir.
IJ will show coverage for all loaded modules and a Coverage Tool Window plus render coverage percents for each file in Project View.
This also should work with reports generated on CI.

## How to generate reports

To generate the HTML or XML report, we use Jacoco CLI.
N.B. The coverage should be collected before generating the report.
To generate a report call

```bash
./gradlew :kotlin-gradle-plugin-test-coverage:jacocoFunctionalHtmlReport // only functional tests HTML report
./gradlew :kotlin-gradle-plugin-test-coverage:jacocoIntegrationHtmlReport // only integration tests HTML report
./gradlew :kotlin-gradle-plugin-test-coverage:jacocoCombinedHtmlReport // both functional and integration HTML report
./gradlew :kotlin-gradle-plugin-test-coverage:jacocoHtmlReport // all HTML report
./gradlew :kotlin-gradle-plugin-test-coverage:jacocoXmlReport // only XML report
./gradlew :kotlin-gradle-plugin-test-coverage:jacocoReport // all reports
```

If there is no coverage for a particular kind of tests, the report generation will be skipped.

