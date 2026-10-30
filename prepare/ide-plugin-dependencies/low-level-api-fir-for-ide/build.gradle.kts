plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":analysis:low-level-api-fir",
        ":analysis:low-level-api-fir-scripting",
    )
)
