import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    hostNativeTarget("native") ?: error("Unexpected host: ${nativeHost.name}")
}

dependencies {
    add(
        "kotlinNativeCompilerPluginClasspath",
        project(":compiler-plugin")
    )
}
