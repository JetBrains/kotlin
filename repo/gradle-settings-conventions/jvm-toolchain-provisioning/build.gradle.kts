import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

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

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
    jvmToolchain(17)
}
