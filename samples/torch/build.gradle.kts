import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
}

val kotlinNativeDataPath = System.getenv("KONAN_DATA_DIR")?.let { File(it) }
    ?: File(System.getProperty("user.home")).resolve(".konan")

val torchHome = kotlinNativeDataPath.resolve("third-party/torch")

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

kotlin {
    targetFromPreset(hostPreset, "torch") {
        binaries {
            executable {
                entryPoint = "sample.torch.main"
                linkerOpts("-L${torchHome.resolve("lib")}", "-lATen")
                runTask?.environment(
                    "LD_LIBRARY_PATH" to torchHome.resolve("lib"),
                    "DYLD_LIBRARY_PATH" to torchHome.resolve("lib")
                )
            }
        }
        compilations["main"].cinterops {
            val torch by creating {
                includeDirs(
                    torchHome.resolve("/include"),
                    torchHome.resolve("/include/TH")
                )
            }
        }
    }
}

val downloadTorch by tasks.creating(Exec::class) {
    workingDir = projectDir
    commandLine("./downloadTorch.sh")
}

val torch: KotlinNativeTarget by kotlin.targets
tasks[torch.compilations["main"].cinterops["torch"].interopProcessingTaskName].dependsOn(downloadTorch)

val downloadMNIST by tasks.creating(Exec::class) {
    workingDir = projectDir
    commandLine("./downloadMNIST.sh")
}

NativeBuildType.values()
    .mapNotNull { torch.binaries.getExecutable(it).runTask }
    .forEach { runTask -> runTask.dependsOn(downloadMNIST) }
