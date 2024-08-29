plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishJarsForIde(listOf(":kotlin-gradle-statistics"))
