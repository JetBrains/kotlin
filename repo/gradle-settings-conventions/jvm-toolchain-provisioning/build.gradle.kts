plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.gradle.toolchains:foojay-resolver:0.4.0")
}

kotlin.jvmToolchain(8)
