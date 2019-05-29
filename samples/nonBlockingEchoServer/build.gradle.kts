import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset

plugins {
    kotlin("multiplatform")
}

// Determine host preset.
val hostOs = System.getProperty("os.name")

val hostPreset: KotlinNativeTargetPreset = when {
    hostOs == "Mac OS X" -> "macosX64"
    hostOs == "Linux" -> "linuxX64"
    // Windows is not supported
    else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
}.let {
    kotlin.presets[it] as KotlinNativeTargetPreset
}

kotlin {
    targetFromPreset(hostPreset, "nonBlockingEchoServer") {
        binaries {
            executable {
                entryPoint = "sample.nbechoserver.main"
                runTask?.args(3000)
            }
        }
    }
}