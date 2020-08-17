plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    jcenter()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx.html/") }
}

kotlin {

    val macos = macosX64("macos64")
    val linux = linuxX64("linux64")
    val windows = mingwX64("mingw64")

    sourceSets {
        val commonNative by creating {}

        windows.compilations["main"].defaultSourceSet {
            dependsOn(commonNative)
        }
        linux.compilations["main"].defaultSourceSet {
            dependsOn(commonNative)
        }
        macos.compilations["main"].defaultSourceSet {
            dependsOn(commonNative)
        }
    }


    configure(listOf(macos, linux, windows)) {
        compilations.all {
            kotlinOptions.verbose = true
            enableEndorsedLibs = true
        }
        binaries {
            executable()
        }
    }
}
