plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

/**
 * Tests:
 *
 *  - libraries - static and shared libraries: building, export, UTD.
 *
 *  - frameworks - building, export, UTD.
 *          + bitcode embedding?
 *          + statics
 *  - executables - building, running, UTD.
 *          + flags, basename and entry points.
 *  - DSL - all ways to create binaries, getters, Groovy + Kotlin.
 *
 *
 */


repositories {
    mavenLocal()
    jcenter()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx.html/") }
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
    val linux = linuxX64("linux64")
    val windows = mingwX64("mingw64")

    configure(listOf(macos, linux, windows)) {
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

    iosArm64("ios") {
        binaries {
            framework {
                export(project(":exported"))
            }
            framework("custom", listOf(RELEASE)) {
                embedBitcode("disable")
                linkerOpts = mutableListOf("-L.")
                freeCompilerArgs = mutableListOf("-Xtime")
                isStatic = true
            }
        }
    }

    iosX64("iosSim") {
        binaries {
            framework()
        }
    }
}
