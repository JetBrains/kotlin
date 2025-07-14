plugins {
    kotlin("jvm")
}

publishTestJarsForIde(
    projectNames = listOf(
        ":compiler:test-infrastructure-utils",
        ":compiler:tests-spec"
    ),
    projectWithFixturesNames = listOf(
        ":compiler:tests-compiler-utils",
        ":compiler:test-infrastructure",
        ":compiler:tests-common-new",
    )
)
