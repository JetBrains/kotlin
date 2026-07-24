plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":analysis:analysis-api-standalone:analysis-api-standalone-base",
        ":analysis:analysis-api-standalone:analysis-api-fir-standalone-base",
        ":analysis:analysis-api-standalone",
    )
)
