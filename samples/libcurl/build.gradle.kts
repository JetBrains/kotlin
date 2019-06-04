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

val mingwPath = File(System.getenv("MINGW64_DIR") ?: "C:/msys64/mingw64")

kotlin {

    // Determine host preset.
    val hostOs = System.getProperty("os.name")

    // Create target for the host platform.
    val hostTarget = when {
        hostOs == "Mac OS X" -> macosX64("libcurl")
        hostOs == "Linux" -> linuxX64("libcurl")
        hostOs.startsWith("Windows") -> mingwX64("libcurl")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    hostTarget.apply {
        compilations["main"].cinterops {
            val libcurl by creating {
                when (preset) {
                    presets["macosX64"] -> includeDirs.headerFilterOnly("/opt/local/include", "/usr/local/include")
                    presets["linuxX64"] -> includeDirs.headerFilterOnly("/usr/include", "/usr/include/x86_64-linux-gnu")
                    presets["mingwX64"] -> includeDirs.headerFilterOnly(mingwPath.resolve("include"))
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

    // Enable experimental stdlib API used by the sample.
    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
    }
}
