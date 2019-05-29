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
    targetFromPreset(hostPreset, "videoPlayer") {
        binaries {
            executable {
                entryPoint = "sample.videoplayer.main"

                when (hostPreset.konanTarget) {
                    MACOS_X64 -> linkerOpts("-L/opt/local/lib", "-L/usr/local/lib")
                    LINUX_X64 -> linkerOpts("-L/usr/lib/x86_64-linux-gnu", "-L/usr/lib64")
                    MINGW_X64 -> linkerOpts(mingwPath.resolve("lib").toString())
                }
            }
        }

        compilations["main"].cinterops {
            val ffmpeg by creating {
                when (hostPreset.konanTarget) {
                    MACOS_X64 -> includeDirs.headerFilterOnly("/opt/local/include", "/usr/local/include")
                    LINUX_X64 -> includeDirs.headerFilterOnly("/usr/include", "/usr/include/x86_64-linux-gnu", "/usr/include/ffmpeg")
                    MINGW_X64 -> includeDirs(mingwPath.resolve("/include"))
                }
            }
            val sdl by creating {
                when (hostPreset.konanTarget) {
                    MACOS_X64 -> includeDirs("/opt/local/include/SDL2", "/usr/local/include/SDL2")
                    LINUX_X64 -> includeDirs("/usr/include/SDL2")
                    MINGW_X64 -> includeDirs(mingwPath.resolve("/include/SDL2"))
                }
            }
        }
    }
}
