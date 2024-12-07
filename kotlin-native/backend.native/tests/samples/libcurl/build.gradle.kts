import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    kotlin("multiplatform")
}

val mingwPath = File(System.getenv("MINGW64_DIR") ?: "C:/msys64/mingw64")

kotlin {

    // Determine host preset.
    val hostOs = System.getProperty("os.name")

    // Create target for the host platform.
    val hostTarget = when {
        hostOs == "Mac OS X" -> macosX64("libcurl")
        hostOs == "Linux" -> linuxX64("libcurl")
        hostOs.startsWith("Windows") -> mingwX64("libcurl")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    hostTarget.apply {
        compilations["main"].cinterops {
            val libcurl by creating {
                when (konanTarget) {
                    KonanTarget.MACOS_X64 -> includeDirs.headerFilterOnly("/opt/local/include", "/usr/local/include")
                    KonanTarget.LINUX_X64 -> includeDirs.headerFilterOnly("/usr/include", "/usr/include/x86_64-linux-gnu")
                    KonanTarget.MINGW_X64 -> includeDirs.headerFilterOnly(mingwPath.resolve("include"))
                }
            }
        }
    }

    // Enable experimental stdlib API used by the sample.
    sourceSets.all {
        languageSettings.optIn("kotlin.ExperimentalStdlibApi")
    }
}
