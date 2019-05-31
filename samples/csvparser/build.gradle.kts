plugins {
    kotlin("multiplatform")
}

kotlin {
    // Determine host preset.
    val hostOs = System.getProperty("os.name")

    // Create target for the host platform.
    val hostTarget = when {
        hostOs == "Mac OS X" -> macosX64("csvParser")
        hostOs == "Linux" -> linuxX64("csvParser")
        hostOs.startsWith("Windows") -> mingwX64("csvParser")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }
        
    hostTarget.apply {
        binaries {
            executable {
                entryPoint = "sample.csvparser.main"
                runTask?.args("./European_Mammals_Red_List_Nov_2009.csv", 4, 100)
            }
        }
    }
}
