plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":analysis:analysis-api-impl-barebone",
        ":analysis:analysis-api-impl-base",
        ":analysis:analysis-internal-utils"
    )
)
