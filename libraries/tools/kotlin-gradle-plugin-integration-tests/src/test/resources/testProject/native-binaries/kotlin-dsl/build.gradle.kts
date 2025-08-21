plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    sourceSets["commonMain"].apply {
        dependencies {
            api(project(":exported"))
        }
    }

    sourceSets["commonTest"].apply {
        dependencies {
            api("org.jetbrains.kotlin:kotlin-test")
        }
    }

    val macos = macosX64("macos64")
    val macosArm = macosArm64("macosArm64")
    val linux = linuxX64("linux64")
    val windows = mingwX64("mingw64")

    configure(listOf(macos, macosArm, linux, windows)) {
        compilerOptions.verbose.set(true)

        compilations["test"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-nowarn")
        }
        binaries {

            executable()                       // Executable with default name.
            executable("foo")                  // Custom binary name.
            executable("bar", listOf(RELEASE)) // Custom build types.

            // Configure a binary.
            executable("baz") {
                // Rename an output binary: baz.kexe -> my-baz.kexe.
                baseName = "my-baz"
                // Use a custom entry point.
                entryPoint = "foo.main"
            }

            executable("test2") {
                compilation = compilations["test"]
                freeCompilerArgs += "-tr"
                linkTaskProvider.configure {
                    toolOptions {
                        freeCompilerArgs.add("-Xtime")
                    }
                }
            }

            sharedLib(listOf(RELEASE)) {
                export(project(":exported"))
            }
            staticLib(listOf(RELEASE)) {
                export(project(":exported"))
            }
        }
        // Check that we can access binaries/tasks:
        // Just by name:
        println("Check link task: ${binaries["releaseShared"].linkTaskProvider.name}")
        // Using a typed getter:
        println("Check run task: ${binaries.getExecutable("foo", RELEASE).runTask?.name}")
    }
}
