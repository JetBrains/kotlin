plugins {
    kotlin("multiplatform")
}

kotlin {
    // Determine host preset.
    val hostOs = System.getProperty("os.name")

    // Create target for the host platform.
    val hostTarget = when {
        hostOs == "Mac OS X" -> macosX64("globalState")
        hostOs == "Linux" -> linuxX64("globalState")
        hostOs.startsWith("Windows") -> mingwX64("globalState")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    hostTarget.apply {
        binaries {
            executable {
                entryPoint = "sample.globalstate.main"
            }
        }
        compilations["main"].cinterops {
            val global by creating
        }
    }
}
