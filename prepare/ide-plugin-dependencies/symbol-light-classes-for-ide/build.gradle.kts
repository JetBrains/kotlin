plugins {
    id("root-config")
    kotlin("jvm")
}

publishJarsForIde(listOf(":analysis:symbol-light-classes"))
