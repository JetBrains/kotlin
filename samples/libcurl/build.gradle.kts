import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.jetbrains.kotlin.sample.native"
version = "1.0"

val localRepo = rootProject.file("build/.m2-local")

publishing {
    repositories {
        maven("file://$localRepo")
    }
}

val cleanLocalRepo by tasks.creating(Delete::class) {
    delete(localRepo)
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
    targetFromPreset(hostPreset, "libcurl") {
        compilations["main"].cinterops {
            val libcurl by creating {
                when (hostPreset.konanTarget) {
                    MACOS_X64 -> includeDirs.headerFilterOnly("/opt/local/include", "/usr/local/include")
                    LINUX_X64 -> includeDirs.headerFilterOnly("/usr/include", "/usr/include/x86_64-linux-gnu")
                    MINGW_X64 -> includeDirs.headerFilterOnly(mingwPath.resolve("/include"))
                }
            }
        }

        mavenPublication {
            pom {
                 withXml {
                     val root = asNode()
                     root.appendNode("name", "libcurl interop library")
                     root.appendNode("description", "A library providing interoperability with host libcurl")
                 }
            }
        }
    }
}
