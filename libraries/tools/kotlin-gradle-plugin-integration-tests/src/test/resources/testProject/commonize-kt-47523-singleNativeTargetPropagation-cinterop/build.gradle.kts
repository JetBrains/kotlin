import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform") apply true
}

repositories {
    mavenLocal()
    mavenCentral()
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    val nativePlatform = hostNativeTarget("nativePlatform") ?: throw IllegalStateException("Unsupported host")

    val commonMain = sourceSets.getByName("commonMain")
    val nativePlatformMain = sourceSets.getByName("nativePlatformMain")
    val nativeMain = sourceSets.create("nativeMain")

    nativeMain.dependsOn(commonMain)
    nativePlatformMain.dependsOn(nativeMain)

    nativePlatform.compilations.getByName("main").cinterops.create("dummy") {
        headers("libs/include/dummy.h")
        compilerOpts.add("-Ilibs/include")
    }
}