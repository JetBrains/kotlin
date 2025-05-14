plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven(url = "file:///dump")
    mavenCentral()
    gradlePluginPortal()
}

kotlin.jvmToolchain(8)
