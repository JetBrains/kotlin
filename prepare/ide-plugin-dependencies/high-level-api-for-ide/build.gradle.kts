plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishJarsForIde(listOf(":analysis:analysis-api"))
