plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishJarsForIde(listOf(":analysis:low-level-api-fir"))
