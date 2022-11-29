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
                jvm()

                group("native") {
                    group("appleAndLinux") {
                        anyApple()
                        anyLinux()
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
    mingwX86()

    sourceSets.all {
        languageSettings.optIn("kotlin.RequiresOptIn")
    }

    targets.withType<KotlinNativeTarget>().forEach { target ->
        target.compilations.getByName("main").cinterops.create("withPosix") {
            header(file("libs/withPosix.h"))
        }
    }
}
