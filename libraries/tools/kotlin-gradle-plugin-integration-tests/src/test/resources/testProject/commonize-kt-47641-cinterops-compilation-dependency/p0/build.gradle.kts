import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()
    macosArm64()
    mingwX64("windowsX64")
}
