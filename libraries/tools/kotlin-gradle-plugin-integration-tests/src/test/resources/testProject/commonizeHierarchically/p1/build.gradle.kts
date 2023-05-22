import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    targetHierarchy.default {
        common {
            group("concurrent") {
                withJvm()

                group("native") {
                    group("appleAndLinux") {
                        withApple()
                        withLinux()
                    }
                }
            }
        }
    }

    js()
    jvm()

    linuxX64()
    linuxArm64()

    macosX64()
    macosArm64()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    mingwX64()
    @Suppress("DEPRECATION_ERROR")
    mingwX86()

    sourceSets.all {
        languageSettings.optIn("kotlin.RequiresOptIn")
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }

    targets.withType<KotlinNativeTarget>().forEach { target ->
        target.compilations.getByName("main").cinterops.create("withPosix") {
            header(file("libs/withPosix.h"))
        }
    }
}
