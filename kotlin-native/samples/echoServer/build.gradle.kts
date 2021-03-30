import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset

plugins {
    kotlin("multiplatform")
}

// Add two additional presets for Raspberry Pi and Linux/ARM64.
val raspberryPiPresets: List<KotlinNativeTargetPreset> = listOf("linuxArm32Hfp", "linuxArm64").map {
    kotlin.presets[it] as KotlinNativeTargetPreset
}

kotlin {
    // Determine host preset.
    val hostOs = System.getProperty("os.name")

    // Create a target for the host platform.
    val hostTarget = when {
        hostOs == "Mac OS X" -> macosX64("echoServer")
        hostOs == "Linux" -> linuxX64("echoServer")
        hostOs.startsWith("Windows") -> mingwX64("echoServer")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    // Create cross-targets.
    val raspberryPiTargets = raspberryPiPresets.map { preset ->
        val targetName = "echoServer${preset.name.capitalize()}"
        targetFromPreset(preset, targetName) {}
    }

    // Configure executables for all targets.
    configure(raspberryPiTargets + listOf(hostTarget)) {
        binaries {
            executable {
                entryPoint = "sample.echoserver.main"
                runTask?.args(3000)
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

    // Enable experimental stdlib API used by the sample.
    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
    }
}
