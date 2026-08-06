plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":analysis:analysis-api-fir",
        ":analysis:analysis-api-fir-diagnostics",
    )
)
