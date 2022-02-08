plugins {
    kotlin("jvm")
}

publishJarsForIde(listOf(":plugins:parcelize:parcelize-compiler", ":plugins:parcelize:parcelize-runtime"))
