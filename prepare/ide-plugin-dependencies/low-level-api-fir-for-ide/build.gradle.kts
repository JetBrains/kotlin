plugins {
    id("root-config")
    kotlin("jvm")
}

publishJarsForIde(listOf(":analysis:low-level-api-fir"))
