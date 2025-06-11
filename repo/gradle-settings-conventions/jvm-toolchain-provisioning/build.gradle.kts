plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    gradlePluginPortal()
}

dependencies {
    implementation(libs.gradle.toolchainsFoojayResolver.gradlePlugin)
}

kotlin.jvmToolchain(8)
