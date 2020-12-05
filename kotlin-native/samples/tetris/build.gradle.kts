import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

val kotlinNativeDataPath = System.getenv("KONAN_DATA_DIR")?.let { File(it) }
    ?: File(System.getProperty("user.home")).resolve(".konan")
val mingw64Path = File(System.getenv("MINGW64_DIR") ?: "C:/msys64/mingw64")
val mingw32Path = File(System.getenv("MINGW32_DIR") ?: "C:/msys64/mingw32")

kotlin {
    val hostOs = System.getProperty("os.name")
    if (hostOs == "Mac OS X") {
        macosX64()
    }
    if (hostOs == "Linux") {
        linuxX64()
    }
    if (hostOs.startsWith("Windows")) {
        mingwX64()
        mingwX86()
    }
    linuxArm32Hfp()

    targets.withType<KotlinNativeTarget> {
        sourceSets["${targetName}Main"].apply {
            kotlin.srcDir("src/tetrisMain/kotlin")
        }

        binaries {
            executable {
                entryPoint = "sample.tetris.main"

                // Compile Windows Resources
                if (preset == presets["mingwX64"] || preset == presets["mingwX86"]) {
                    val taskName = linkTaskName.replaceFirst("link", "windres")
                    val inFile = File("src/tetrisMain/resources/Tetris.rc")
                    val outFile = buildDir.resolve("processedResources/$taskName.res")
                    val windresTask = tasks.register<Exec>(taskName) {
                        val llvmDir = when (preset) {
                            presets["mingwX86"] -> kotlinNativeDataPath.resolve(
                                    "dependencies/msys2-mingw-w64-i686-clang-llvm-lld-compiler_rt-8.0.1/bin")
                            presets["mingwX64"] -> kotlinNativeDataPath.resolve(
                                    "dependencies/msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1/bin")
                            else -> throw GradleException("Unsupported presets")
                        }.toString()
                        inputs.file(inFile)
                        outputs.file(outFile)
                        commandLine("$llvmDir/windres", inFile, "-O", "coff", "-o", outFile)
                        environment("PATH", "$llvmDir;${System.getenv("PATH")}")
                        dependsOn(compilation.compileKotlinTask)
                    }
                    linkTask.dependsOn(windresTask)
                    linkerOpts(outFile.toString())
                }

                when (preset) {
                    presets["macosX64"] -> linkerOpts("-L/opt/local/lib", "-L/usr/local/lib", "-lSDL2")
                    presets["linuxX64"] -> linkerOpts("-L/usr/lib64", "-L/usr/lib/x86_64-linux-gnu", "-lSDL2")
                    presets["mingwX64"] -> linkerOpts(
                        "-L${mingw64Path.resolve("lib")}",
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
                    presets["mingwX86"] -> linkerOpts(
                        "-L${mingw32Path.resolve("lib")}",
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
                    presets["linuxArm32Hfp"] -> linkerOpts("-lSDL2")
                }

                val distTaskName = linkTaskName.replaceFirst("link", "dist")
                val distTask = tasks.register<Copy>(distTaskName) {
                    from("src/tetrisMain/resources")
                    into(linkTask.outputFile.get().parentFile)
                    exclude("*.rc")
                    if (!konanTarget.family.isAppleFamily) {
                        exclude("*.plist")
                    }
                    dependsOn(linkTask)
                }
                tasks["assemble"].dependsOn(distTask)

                runTask?.workingDir(project.provider { outputDirectory })
            }
        }

        compilations["main"].cinterops {
            val sdl by creating {
                when (preset) {
                    presets["macosX64"] -> includeDirs("/opt/local/include/SDL2", "/usr/local/include/SDL2")
                    presets["linuxX64"] -> includeDirs("/usr/include", "/usr/include/x86_64-linux-gnu", "/usr/include/SDL2")
                    presets["mingwX64"] -> includeDirs(mingw64Path.resolve("include/SDL2"))
                    presets["mingwX86"] -> includeDirs(mingw32Path.resolve("include/SDL2"))
                    presets["linuxArm32Hfp"] -> includeDirs(kotlinNativeDataPath.resolve("dependencies/target-sysroot-2-raspberrypi/usr/include/SDL2"))
                }
            }
        }

        compilations["main"].enableEndorsedLibs = true
    }
}
