import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset

plugins {
    kotlin("multiplatform")
}

// Determine host preset.
val hostOs = System.getProperty("os.name")

val hostPreset: KotlinNativeTargetPreset = when {
    hostOs == "Mac OS X" -> "macosX64"
    hostOs == "Linux" -> "linuxX64"
    // Windows is not supported
    else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
}.let {
    kotlin.presets[it] as KotlinNativeTargetPreset
}

val kotlinNativeDataPath = System.getenv("KONAN_DATA_DIR")?.let { File(it) }
    ?: File(System.getProperty("user.home")).resolve(".konan")

val tensorflowHome = kotlinNativeDataPath.resolve("third-party/tensorflow")

kotlin {
    targetFromPreset(hostPreset, "tensorflow") {
        binaries {
            executable {
                entryPoint = "sample.tensorflow.main"
                linkerOpts("-L${tensorflowHome.resolve("lib")}", "-ltensorflow")
                runTask?.environment(
                    "LD_LIBRARY_PATH" to tensorflowHome.resolve("lib"),
                    "DYLD_LIBRARY_PATH" to tensorflowHome.resolve("lib")
                )
            }
        }
        compilations["main"].cinterops {
            val tensorflow by creating {
                includeDirs(tensorflowHome.resolve("/include"))
            }
        }
    }
}

val downloadTensorflow by tasks.creating(Exec::class) {
    workingDir = projectDir
    commandLine("./downloadTensorflow.sh")
}

val tensorflow: KotlinNativeTarget by kotlin.targets
tasks[tensorflow.compilations["main"].cinterops["tensorflow"].interopProcessingTaskName].dependsOn(downloadTensorflow)
