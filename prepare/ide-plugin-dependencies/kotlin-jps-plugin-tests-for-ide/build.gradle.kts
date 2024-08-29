plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishTestJarsForIde(
    listOf(
        ":jps:jps-plugin",
        ":kotlin-build-common",
    )
)
