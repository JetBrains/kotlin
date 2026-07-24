plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishTestJarsForIde(
    projectNames = emptyList(),
    projectWithFixturesNames = listOf(":compiler:incremental-compilation-impl"),
)
