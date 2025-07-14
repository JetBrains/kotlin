plugins {
    kotlin("jvm")
}

publishTestJarsForIde(
    projectNames = listOf(
        ":compiler:test-infrastructure-utils",
        ":compiler:tests-compiler-utils",
        ":compiler:tests-spec"
    ),
    projectWithFixturesNames = listOf(
        ":compiler:test-infrastructure",
        ":compiler:tests-common-new",
    )
)
