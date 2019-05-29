import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset
import org.jetbrains.kotlin.konan.target.KonanTarget.*

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

val mingwPath = File(System.getenv("MINGW64_DIR") ?: "C:/msys64/mingw64")

kotlin {
    targetFromPreset(hostPreset, "gitChurn") {
        binaries {
            executable {
                entryPoint = "sample.gitchurn.main"
                if (hostPreset.konanTarget == MINGW_X64) {
                    linkerOpts(mingwPath.resolve("lib").toString())
                    runTask?.environment("PATH" to mingwPath.resolve("bin"))
                }
                runTask?.args(rootProject.rootDir.resolve(".."))
            }
        }
        compilations["main"].cinterops {
            val libgit2 by creating {
                when (hostPreset.konanTarget) {
                    MACOS_X64 -> includeDirs.headerFilterOnly("/opt/local/include", "/usr/local/include")
                    LINUX_X64 -> includeDirs.headerFilterOnly("/usr/include")
                    MINGW_X64 -> includeDirs.headerFilterOnly(mingwPath.resolve("include"))
                }
            }
        }
    }
}
