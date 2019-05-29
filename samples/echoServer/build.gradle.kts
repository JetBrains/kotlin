import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset

plugins {
    kotlin("multiplatform")
}

// Determine host preset.
val hostOs = System.getProperty("os.name")

val hostPreset: KotlinNativeTargetPreset = when {
    hostOs == "Mac OS X" -> "macosX64"
    hostOs == "Linux" -> "linuxX64"
    hostOs.startsWith("Windows") -> "mingwX64"
    else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
}.let {
    kotlin.presets[it] as KotlinNativeTargetPreset
}

// Add two additional presets for Raspberry Pi.
val raspberryPiPresets: List<KotlinNativeTargetPreset> = listOf("linuxArm32Hfp", "linuxArm64").map {
    kotlin.presets[it] as KotlinNativeTargetPreset
}

kotlin {
    targetFromPreset(hostPreset, "echoServer") {
        binaries {
            executable {
                entryPoint = "sample.echoserver.main"
                runTask?.args(3000)
            }
        }
    }

    raspberryPiPresets.forEach { preset ->
        val targetName = "echoServer${preset.name.capitalize()}"
        targetFromPreset(preset, targetName) {
            binaries {
                executable {
                    entryPoint = "sample.echoserver.main"
                    runTask?.args(3000)
                }
            }
        }
    }

    sourceSets {
        val echoServerMain by getting
        raspberryPiPresets.forEach { preset ->
            val mainSourceSetName = "echoServer${preset.name.capitalize()}Main"
            getByName(mainSourceSetName).dependsOn(echoServerMain)
        }
    }
}
