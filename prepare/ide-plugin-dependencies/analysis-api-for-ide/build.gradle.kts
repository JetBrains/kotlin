plugins {
    id("root-config")
    kotlin("jvm")
}

publishJarsForIde(listOf(":analysis:analysis-api"))
