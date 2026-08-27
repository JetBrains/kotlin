plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(listOf(":analysis:analysis-api-platform-interface"))
