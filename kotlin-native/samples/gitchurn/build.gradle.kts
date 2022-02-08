plugins {
    kotlin("multiplatform")
}

val mingwPath = File(System.getenv("MINGW64_DIR") ?: "C:/msys64/mingw64")

kotlin {
    // Determine host preset.
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    // Create a target for the host platform.
    val hostTarget = when {
        hostOs == "Mac OS X" -> macosX64("gitChurn")
        hostOs == "Linux" -> linuxX64("gitChurn")
        isMingwX64 -> mingwX64("gitChurn")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    hostTarget.apply {
        binaries {
            executable {
                entryPoint = "sample.gitchurn.main"
                if (isMingwX64) {
                    linkerOpts("-L${mingwPath.resolve("lib")}")
                    runTask?.environment("PATH" to mingwPath.resolve("bin"))
                }
                runTask?.args(rootProject.rootDir.resolve(".."))
            }
        }
        compilations["main"].cinterops {
            val libgit2 by creating {
                when (preset) {
                    presets["macosX64"] -> includeDirs.headerFilterOnly("/opt/local/include", "/usr/local/include")
                    presets["linuxX64"] -> includeDirs.headerFilterOnly("/usr/include")
                    presets["mingwX64"] -> includeDirs.headerFilterOnly(mingwPath.resolve("include"))
                }
            }
        }
    }
}
