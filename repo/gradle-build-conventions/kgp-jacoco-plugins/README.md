# kgp-jacoco-plugins

Build conventions for collecting KGP test coverage with JaCoCo.
See also [`kotlin-gradle-plugin-test-coverage/README.md`](../../../libraries/tools/kotlin-gradle-plugin-test-coverage/README.md)

## `kgp-jacoco-instrumenter`

Replaces the output of the `jar`/`embeddableJar` tasks with an offline-instrumented version produced by the JaCoCo CLI,
so tests running against these jars collect coverage data
Also adds the `common` source set to `mainSourceElements` so its sources appear in aggregated reports.

## `kgp-jacoco-agent-setup`

Set up JaCoCo agent for KGP integration tests that are using Gradle TestKit. 
Passes the location of the JaCoCo agent runtime jar and the coverage output file (`build/jacoco/coverage.exec`)
to test JVMs via the `jacocoRuntimeJar`/`jacocoDestFile` system properties.
