plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishJarsForIde(
    listOf(
        ":analysis:analysis-api-impl-barebone",
        ":analysis:analysis-api-impl-base",
        ":analysis:analysis-internal-utils"
    )
)
