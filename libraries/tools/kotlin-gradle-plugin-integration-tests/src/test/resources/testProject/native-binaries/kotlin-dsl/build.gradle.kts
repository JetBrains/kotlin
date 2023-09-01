plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

// KT-45801
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    kotlinOptions
}

kotlin {
    sourceSets["commonMain"].apply {
        dependencies {
            api("org.jetbrains.kotlin:kotlin-stdlib-common")
            api(project(":exported"))
        }
    }

    sourceSets["commonTest"].apply {
        dependencies {
            api("org.jetbrains.kotlin:kotlin-test-annotations-common")
        }
    }

    val macos = macosX64("macos64")
    val macosArm = macosArm64("macosArm64")
    val linux = linuxX64("linux64")
    val windows = mingwX64("mingw64")

    configure(listOf(macos, macosArm, linux, windows)) {
        compilations.all { kotlinOptions.verbose = true }
        compilations["test"].kotlinOptions.freeCompilerArgs += "-nowarn"
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
                linkTask.kotlinOptions {
                    freeCompilerArgs += "-Xtime"
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
        println("Check link task: ${binaries["releaseShared"].linkTask.name}")
        // Using a typed getter:
        println("Check run task: ${binaries.getExecutable("foo", RELEASE).runTask?.name}")
    }
}
