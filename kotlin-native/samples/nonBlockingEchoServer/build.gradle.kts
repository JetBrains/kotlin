plugins {
    kotlin("multiplatform")
}

kotlin {
    // Determine host preset.
    val hostOs = System.getProperty("os.name")

    // Create target for the host platform.
    val hostTarget = when {
        hostOs == "Mac OS X" -> macosX64("nonBlockingEchoServer")
        hostOs == "Linux" -> linuxX64("nonBlockingEchoServer")
        hostOs.startsWith("Windows") -> mingwX64("nonBlockingEchoServer")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    hostTarget.apply {
        binaries {
            executable {
                entryPoint = "sample.nbechoserver.main"
                runTask?.args(3000)
            }
        }
    }
}