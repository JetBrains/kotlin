plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishTestJarsForIde(
    projectNames = listOf(
        ":compiler:tests-spec"
    ),
    projectWithFixturesNames = listOf(
        ":compiler:tests-compiler-utils",
        ":compiler:test-infrastructure-utils.common",
        ":compiler:test-infrastructure-utils",
        ":compiler:test-infrastructure",
        ":compiler:tests-common-new",
    )
)
