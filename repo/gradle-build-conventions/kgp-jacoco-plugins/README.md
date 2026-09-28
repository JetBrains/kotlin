# kgp-jacoco-plugins

Build conventions for collecting KGP test coverage with JaCoCo.
See also [`kotlin-gradle-plugin-test-coverage/README.md`](../../../libraries/tools/kotlin-gradle-plugin-test-coverage/README.md)

## `kgp-jacoco-instrumenter`

Replaces the output of the `jar`/`embeddableJar` tasks with an offline-instrumented version produced by the JaCoCo CLI,
so tests running against these jars collect coverage data

## `kgp-jacoco-on-the-fly`

Required for projects with sources where you want to measure test coverage,
tests that can be handled by JaCoCo on the fly (basically without TestKit) or both.
The plugin sets up `jacoco` gradle plugin, adds the `common` source set to `mainSourceElements` so its sources appear in aggregated reports.
You may also need to apply `kgp-jacoco-instrumenter` plugin if the coverage for the project is measured by tests with TestKit.

## `kgp-jacoco-offline`

Required for projects with tests where Gradle TestKit is used. So far, there is only KGP Integration Tests.
Passes the location of the JaCoCo agent runtime jar and the coverage output file (`build/jacoco/coverage.exec`)
to test JVMs via the `jacocoRuntimeJar`/`jacocoDestFile` system properties.
