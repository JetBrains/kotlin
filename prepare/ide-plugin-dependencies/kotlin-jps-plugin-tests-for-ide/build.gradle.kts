plugins {
    kotlin("jvm")
}

publishTestJarsForIde(
    listOf(
        ":jps:jps-plugin",
        ":kotlin-build-common",
    )
)
