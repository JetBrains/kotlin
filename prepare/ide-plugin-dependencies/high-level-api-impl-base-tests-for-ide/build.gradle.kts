plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishTestJarsForIde(listOf(":analysis:analysis-api-impl-barebone", ":analysis:analysis-api-impl-base"))
