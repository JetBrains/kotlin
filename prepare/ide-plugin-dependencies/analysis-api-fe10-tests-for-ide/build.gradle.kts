plugins {
    id("root-config")
    kotlin("jvm")
}

publishTestJarsForIde(listOf(":analysis:analysis-api-fe10"))
