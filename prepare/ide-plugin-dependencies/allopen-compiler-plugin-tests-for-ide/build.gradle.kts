plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishTestJarsForIde(listOf(":compiler:incremental-compilation-impl"))
