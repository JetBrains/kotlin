plugins {
    kotlin("jvm")
}

publishTestJarsForIde(
    listOf(
        ":jps:jps-plugin",
    )
)
