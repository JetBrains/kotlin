import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}


@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    val nativeTarget = hostNativeTarget("native") ?: error("Unexpected host: ${nativeHost.name}")

    nativeTarget.compilations["main"].cinterops.create("sampleInterop") {
        header(file("src/nativeInterop/cinterop/sampleInterop.h"))
        header(file("src/nativeInterop/cinterop/sampleInteropNoise.h"))
    }
}