plugins {
    id("root-config")
    kotlin("jvm")
}

publishTestJarsForIde(
    projectNames = listOf(),
    projectWithFixturesNames = listOf(":kotlin-build-common"),
    projectWithRenamedTestJarNames = listOf(":kotlin-build-common"),
)
