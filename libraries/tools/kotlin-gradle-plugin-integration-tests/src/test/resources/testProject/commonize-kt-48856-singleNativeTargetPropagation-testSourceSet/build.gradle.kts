import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
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
    sourceSets.all {
        languageSettings {
            optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    val platformTarget = hostNativeTarget("platform") ?: error("Unexpected host: ${nativeHost.name}")

    platformTarget.compilations["main"].cinterops.create("sampleInterop") {
        header(file("src/nativeInterop/cinterop/sampleInterop.h"))
    }

    val platformTest = sourceSets.getByName("platformTest")
    val nativeTest = sourceSets.create("nativeTest")
    platformTest.dependsOn(nativeTest)
}
