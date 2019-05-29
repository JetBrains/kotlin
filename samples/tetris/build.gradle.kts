import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    kotlin("multiplatform")
}

val hostOs = System.getProperty("os.name")
val isLinux = hostOs == "Linux"
val isWindows = hostOs.startsWith("Windows")

// If host platform is Linux and RaspberryPi target is activated.
val isRaspberryPiBuild =
    isLinux && project.findProperty("tetris.raspberrypi.build")?.toString()?.toBoolean() == true

// If host platform is Windows and x86 target is activated.
val isMingwX86Build =
    isWindows && project.findProperty("tetris.mingwX86.build")?.toString()?.toBoolean() == true

val hostPreset = determinePreset()

val winCompiledResourceFile = buildDir.resolve("compiledWindowsResources/Tetris.res")

val kotlinNativeDataPath = System.getenv("KONAN_DATA_DIR")?.let { File(it) }
    ?: File(System.getProperty("user.home")).resolve(".konan")

val mingwPath = if (isMingwX86Build)
    File(System.getenv("MINGW32_DIR") ?: "C:/msys32/mingw32")
else
    File(System.getenv("MINGW64_DIR") ?: "C:/msys64/mingw64")

kotlin {
    targetFromPreset(hostPreset, "tetris") {
        binaries {
            executable {
                entryPoint = "sample.tetris.main"
                when (hostPreset.konanTarget) {
                    MACOS_X64 -> linkerOpts("-L/opt/local/lib", "-L/usr/local/lib", "-lSDL2")
                    LINUX_X64 -> linkerOpts("-L/usr/lib64", "-L/usr/lib/x86_64-linux-gnu", "-lSDL2")
                    MINGW_X64, MINGW_X86 -> linkerOpts(
                        winCompiledResourceFile.toString(),
                        mingwPath.resolve("lib").toString(),
                        "-Wl,-Bstatic",
                        "-lstdc++",
                        "-static",
                        "-lSDL2",
                        "-limm32",
                        "-lole32",
                        "-loleaut32",
                        "-lversion",
                        "-lwinmm",
                        "-lsetupapi",
                        "-mwindows"
                    )
                    LINUX_ARM32_HFP -> linkerOpts("-lSDL2")
                }
                runTask?.workingDir(project.provider {
                    val tetris: KotlinNativeTarget by kotlin.targets
                    tetris.binaries.getExecutable(buildType).outputDirectory
                })
            }
        }
        
        compilations["main"].cinterops {
            val sdl by creating {
                when (hostPreset.konanTarget) {
                    MACOS_X64 -> includeDirs("/opt/local/include/SDL2", "/usr/local/include/SDL2")
                    LINUX_X64 -> includeDirs("/usr/include/SDL2")
                    MINGW_X64, MINGW_X86 -> includeDirs(mingwPath.resolve("/include/SDL2"))
                    LINUX_ARM32_HFP -> includeDirs(kotlinNativeDataPath.resolve("dependencies/target-sysroot-1-raspberrypi/usr/include/SDL2"))
                }
            }
        }
    }
}

val compileWindowsResources: Exec? = if (hostPreset.konanTarget == MINGW_X64 || hostPreset.konanTarget == MINGW_X86) {
    val compileWindowsResources: Exec by tasks.creating(Exec::class) {
        val windresDir = if (isMingwX86Build)
            kotlinNativeDataPath.resolve("dependencies/msys2-mingw-w64-i686-gcc-7.4.0-clang-llvm-6.0.1/bin")
        else
            kotlinNativeDataPath.resolve("dependencies/msys2-mingw-w64-x86_64-gcc-7.3.0-clang-llvm-lld-6.0.1-2/bin")

        val winResourceFile = kotlin.sourceSets["tetrisMain"].resources.files.first { it.name == "Tetris.rc" }
        val path = System.getenv("PATH")

        commandLine(
            windresDir.resolve("windres"),
            winResourceFile,
            "-O", "coff",
            "-o", winCompiledResourceFile
        )
        environment("PATH" to "$windresDir;$path")

        inputs.file(winResourceFile)
        outputs.file(winCompiledResourceFile)
    }

    compileWindowsResources
} else null

afterEvaluate {
    val tetris: KotlinNativeTarget by kotlin.targets
    val linkTasks = NativeBuildType.values().mapNotNull { tetris.binaries.getExecutable(it).linkTask }

    linkTasks.forEach { linkTask ->
        if (compileWindowsResources != null) linkTask.dependsOn(compileWindowsResources)
        linkTask.doLast {
            copy {
                from(kotlin.sourceSets["tetrisMain"].resources)
                into(linkTask.outputFile.get().parentFile)
                exclude("*.rc")
            }
        }
    }
}

fun determinePreset(): KotlinNativeTargetPreset {
    val preset = when {
        isRaspberryPiBuild -> kotlin.presets["linuxArm32Hfp"] as KotlinNativeTargetPreset // aka RaspberryPi
        isMingwX86Build -> kotlin.presets["mingwX86"] as KotlinNativeTargetPreset
        else -> return when {
            hostOs == "Mac OS X" -> "macosX64"
            hostOs == "Linux" -> "linuxX64"
            hostOs.startsWith("Windows") -> "mingwX64"
            else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
        }.let {
            kotlin.presets[it] as KotlinNativeTargetPreset
        }
    }
    println("$project has been configured for ${preset.name} platform.")
    return preset
}
