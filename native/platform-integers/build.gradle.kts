import plugins.configureDefaultPublishing

plugins {
    `maven-publish`
    kotlin("multiplatform")
}

description = "Kotlin Standard Library extension for experimental platform integer types"

kotlin {
    linuxX64()
    linuxArm32Hfp()
    linuxMips32()
    linuxMipsel32()
    linuxArm64()
    iosArm32()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    mingwX86()
    mingwX64()
    macosX64()
    macosArm64()
    watchosArm32()
    watchosArm64()
    watchosX86()
    watchosX64()
    watchosSimulatorArm64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    sourceSets {
        val commonMain by getting

        val intPlatforms by creating {
            dependsOn(commonMain)
        }
        val longPlatforms by creating {
            dependsOn(commonMain)
        }

        val linuxX64Main by getting
        val linuxArm32HfpMain by getting
        val linuxMips32Main by getting
        val linuxMipsel32Main by getting
        val linuxArm64Main by getting
        val macosX64Main by getting
        val iosArm32Main by getting
        val iosArm64Main by getting
        val iosX64Main by getting
        val mingwX64Main by getting
        val mingwX86Main by getting
        val iosSimulatorArm64Main by getting
        val macosArm64Main by getting
        val watchosArm32Main by getting
        val watchosArm64Main by getting
        val watchosX86Main by getting
        val watchosX64Main by getting
        val watchosSimulatorArm64Main by getting
        val tvosArm64Main by getting
        val tvosX64Main by getting
        val tvosSimulatorArm64Main by getting

        val bitness32 = listOf(
            linuxArm32HfpMain,
            linuxArm32HfpMain,
            linuxMips32Main,
            linuxMipsel32Main,
            iosArm32Main,
            mingwX86Main,
            watchosArm32Main,
            watchosX86Main,
        )
        val bitness64 = listOf(
            linuxX64Main,
            macosX64Main,
            iosArm64Main,
            iosX64Main,
            mingwX64Main,
            linuxArm64Main,
            iosSimulatorArm64Main,
            macosArm64Main,
            watchosArm64Main,
            watchosX64Main,
            watchosSimulatorArm64Main,
            tvosArm64Main,
            tvosX64Main,
            tvosSimulatorArm64Main,
        )

        bitness32.forEach { sourceSet ->
            sourceSet.dependsOn(intPlatforms)
        }
        bitness64.forEach { sourceSet ->
            sourceSet.dependsOn(longPlatforms)
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.RequiresOptIn")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xallow-kotlin-package"
    }
}

afterEvaluate {
    kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().flatMap { it.compilations }.forEach { compilation ->
        compilation.compileKotlinTask.setSource(
            compilation.compileKotlinTask.source.filter { "commonMain" !in it.path }
        )
    }
}

configureDefaultPublishing()
