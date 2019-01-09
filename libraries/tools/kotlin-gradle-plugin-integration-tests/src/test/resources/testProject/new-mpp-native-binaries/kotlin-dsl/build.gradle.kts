plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    jcenter()
    maven { setUrl("http://dl.bintray.com/kotlin/kotlinx.html/") }
}

kotlin {
    sourceSets["commonMain"].apply {
        dependencies {
            api("org.jetbrains.kotlin:kotlin-stdlib-common")
        }
    }

    sourceSets.create("iosMain").apply {
        dependencies {
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
            }

            sharedLib(listOf(RELEASE))
            staticLib(listOf(RELEASE))
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
            }
        }
    }
}
