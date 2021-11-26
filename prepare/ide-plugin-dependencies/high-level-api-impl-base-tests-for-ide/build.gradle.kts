plugins {
    kotlin("jvm")
}

publishTestJarsForIde(listOf(":analysis:analysis-api-impl-barebone", ":analysis:analysis-api-impl-base"))
