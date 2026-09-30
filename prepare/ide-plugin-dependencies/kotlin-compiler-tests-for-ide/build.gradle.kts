plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishTestJarsForIde(
    projectWithFixturesNames = listOf(
        ":compiler:tests-spec",
        ":compiler:tests-compiler-utils",
        ":compiler:test-infrastructure-utils.common",
        ":compiler:test-infrastructure-utils",
        ":compiler:test-infrastructure",
        ":compiler:tests-common-new",
    )
)
