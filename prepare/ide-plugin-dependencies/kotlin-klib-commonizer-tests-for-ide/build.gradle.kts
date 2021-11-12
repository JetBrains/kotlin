plugins {
    kotlin("jvm")
}

publishTestJarsForIde(
    listOf(":native:kotlin-klib-commonizer")
)
