import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js().nodejs()
    linuxX64()
    macosX64()
    mingwX64("windowsX64")
    @Suppress("DEPRECATION_ERROR")
    mingwX86("windowsX86")
}
