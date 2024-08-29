plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishJarsForIde(listOf(":analysis:symbol-light-classes"))
