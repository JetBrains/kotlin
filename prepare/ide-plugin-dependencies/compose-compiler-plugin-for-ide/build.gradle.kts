plugins {
    id("root-config")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(":plugins:compose-compiler-plugin:compiler-hosted")
)