plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":analysis:analysis-api-standalone:analysis-api-standalone-fir",
        ":analysis:analysis-api-standalone",
    )
)
