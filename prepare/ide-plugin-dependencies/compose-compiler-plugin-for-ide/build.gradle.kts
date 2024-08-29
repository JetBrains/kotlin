plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishJarsForIde(
    listOf(":plugins:compose-compiler-plugin:compiler-hosted")
)
