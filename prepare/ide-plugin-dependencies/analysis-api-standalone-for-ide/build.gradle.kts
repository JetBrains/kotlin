plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishJarsForIde(
    listOf(
        ":analysis:analysis-api-standalone:analysis-api-standalone-base",
        ":analysis:analysis-api-standalone:analysis-api-fir-standalone-base",
        ":analysis:analysis-api-standalone",
    )
)
