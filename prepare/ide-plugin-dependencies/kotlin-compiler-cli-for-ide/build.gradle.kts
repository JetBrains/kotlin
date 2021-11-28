plugins {
    kotlin("jvm")
}

publishJarsForIde(listOf(
    ":compiler:cli",
    ":compiler:cli-js"
))
