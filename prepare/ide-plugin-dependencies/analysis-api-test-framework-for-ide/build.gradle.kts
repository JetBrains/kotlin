plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishTestJarsForIde(
    projectNames = listOf(),
    projectWithFixturesNames = listOf(
        ":analysis:analysis-api-fir",
        ":analysis:low-level-api-fir",
        ":analysis:analysis-test-framework",
        ":analysis:analysis-api-impl-base",
        ":analysis:analysis-api-standalone",
        ":analysis:decompiled:decompiler-to-file-stubs",
    )
)
