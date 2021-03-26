plugins {
    kotlin("jvm")
}

publishTestJarsForIde(listOf(":compiler:incremental-compilation-impl"))
