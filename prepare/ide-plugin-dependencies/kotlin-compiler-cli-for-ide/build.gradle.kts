plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishJarsForIde(listOf(
    ":compiler:cli",
    ":compiler:cli-js"
))
