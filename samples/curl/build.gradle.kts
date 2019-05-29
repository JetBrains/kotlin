import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    kotlin("multiplatform")
}

val localRepo = rootProject.file("build/.m2-local")

repositories {
    maven("file://$localRepo")
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
    targetFromPreset(hostPreset, "curl") {
        binaries {
            executable {
                entryPoint = "sample.curl.main"
                if (hostPreset.konanTarget == MINGW_X64) {
                    // Add lib path to `libcurl` and its dependencies:
                    linkerOpts(mingwPath.resolve("lib").toString())
                    runTask?.environment("PATH" to mingwPath.resolve("bin"))
                }
                runTask?.args("https://www.jetbrains.com/")
            }
        }
    }

    sourceSets {
        val curlMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin.sample.native:libcurl:1.0")
            }
        }
    }
}

// The code snippet below is needed to make all compile tasks depend on publication of
// "libcurl" library. So that to the time of compilation the library will already be
// in Maven repo and will be successfully resolved as a dependency of this project.
tasks.withType(AbstractCompile::class) {
    dependsOn(":libcurl:publish")
}

// The following snippet is needed to give instructions for IDEA user who just imported project
// and sees "Could not resolve..." message in IDEA console.
gradle.buildFinished {
    val configurationName = kotlin.targets["curl"].compilations["main"].compileDependencyConfigurationName
    val configuration = project.configurations[configurationName]
    if (configuration.isCanBeResolved && configuration.state == Configuration.State.RESOLVED_WITH_FAILURES) {
        println(
            """
                |
                |IMPORTANT:
                |The message about unresolved "libcurl" dependency likely means that "libcurl" has not been built and published to local Maven repo yet.
                |Please run "publish" task for "libcurl" sub-project and re-import Kotlin/Native samples in IDEA.
            """.trimMargin()
        )
    }
}
