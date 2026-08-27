plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishTestJarsForIde(
    projectNames = listOf(),
    projectWithFixturesNames = listOf(":kotlin-build-common"),
    projectWithRenamedTestJarNames = listOf(":kotlin-build-common"),
)
