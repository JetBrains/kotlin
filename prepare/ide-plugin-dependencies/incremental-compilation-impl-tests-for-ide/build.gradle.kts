plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishTestJarsForIde(
    projectWithFixturesNames = listOf(":compiler:incremental-compilation-impl"),
)
