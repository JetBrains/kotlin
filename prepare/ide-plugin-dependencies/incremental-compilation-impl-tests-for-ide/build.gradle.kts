plugins {
    id("root-config")
    kotlin("jvm")
}

publishTestJarsForIde(
    projectNames = emptyList(),
    projectWithFixturesNames = listOf(":compiler:incremental-compilation-impl"),
)
