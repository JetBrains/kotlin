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
    targetFromPreset(hostPreset, "gtk") {
        binaries {
            executable {
                entryPoint = "sample.gtk.main"
                if (hostPreset.konanTarget == MINGW_X64) {
                    linkerOpts(mingwPath.resolve("lib").toString())
                    runTask?.environment("PATH" to mingwPath.resolve("bin"))
                }
            }
        }
        compilations["main"].cinterops {
            val gtk3 by creating {
                when (hostPreset.konanTarget) {
                    MACOS_X64, LINUX_X64 -> {
                        listOf("/opt/local/include", "/usr/include", "/usr/local/include").forEach {
                            includeDirs(
                                "$it/atk-1.0",
                                "$it/gdk-pixbuf-2.0",
                                "$it/cairo",
                                "$it/pango-1.0",
                                "$it/gtk-3.0",
                                "$it/glib-2.0"
                            )
                        }

                        includeDirs(
                            "/opt/local/lib/glib-2.0/include",
                            "/usr/lib/x86_64-linux-gnu/glib-2.0/include",
                            "/usr/local/lib/glib-2.0/include"
                        )
                    }
                    MINGW_X64 -> {
                        listOf(
                            "/include/atk-1.0",
                            "/include/gdk-pixbuf-2.0",
                            "/include/cairo",
                            "/include/pango-1.0",
                            "/include/gtk-3.0",
                            "/include/glib-2.0",
                            "/lib/glib-2.0/include"
                        ).forEach {
                            includeDirs(mingwPath.resolve(it))
                        }
                    }
                }
            }
        }
    }
}