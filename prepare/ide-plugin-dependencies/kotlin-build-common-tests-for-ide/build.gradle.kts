plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishTestJarsForIde(listOf(":kotlin-build-common"))
