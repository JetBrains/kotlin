plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven(url = "file:///dump")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.gradle.toolchainsFoojayResolver.gradlePlugin)
}

kotlin.jvmToolchain(8)
