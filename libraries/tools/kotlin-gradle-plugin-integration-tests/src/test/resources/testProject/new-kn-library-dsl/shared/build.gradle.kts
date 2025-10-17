import org.jetbrains.kotlin.gradle.plugin.cocoapods.withPodspec

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}

@Suppress("DEPRECATION_ERROR")
kotlinArtifacts {
    Native.Library("mylib") {
        target = linuxX64
        toolOptions {
            freeCompilerArgs.add("-Xmen=pool")
        }
    }
    Native.Library("myslib") {
        target = linuxX64
        isStatic = false
        modes(DEBUG)
        addModule(project(":lib"))
        toolOptions {
            verbose.set(false)
            freeCompilerArgs.set(emptyList())
        }
    }
    Native.Framework("myframe") {
        modes(DEBUG, RELEASE)
        target = iosArm64
        isStatic = false
        toolOptions {
            verbose.set(false)
        }
    }
    Native.FatFramework("myfatframe") {
        targets(iosX64, iosSimulatorArm64)
        toolOptions {
            suppressWarnings.set(false)
        }
    }
    Native.XCFramework {
        targets(iosX64, iosArm64, iosSimulatorArm64)
        setModules(
            project(":shared"),
            project(":lib")
        )
    }
}
