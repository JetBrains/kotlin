plugins {
    kotlin("jvm")
}

publishJarsForIde(listOf(
    ":compiler:cli",
    ":compiler:cli-jvm",
    ":compiler:cli-js",
    ":compiler:cli-metadata",
))
