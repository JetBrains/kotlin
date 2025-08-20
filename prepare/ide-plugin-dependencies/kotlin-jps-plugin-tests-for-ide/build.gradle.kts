plugins {
    kotlin("jvm")
}

publishTestJarsForIde(
    projectNames = listOf(":jps:jps-plugin"),
    projectWithFixturesNames = listOf(":kotlin-build-common"),
    projectWithRenamedTestJarNames = listOf(":kotlin-build-common"),
)
