plugins {
    id("root-config")
    kotlin("jvm")
}

publishTestJarsForIde(listOf(":compiler:incremental-compilation-impl"))
