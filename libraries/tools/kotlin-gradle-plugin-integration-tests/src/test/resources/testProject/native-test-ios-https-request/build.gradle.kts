plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    iosSimulatorArm64 {
        compilations.all() {
            compilerOptions.options.apply {
                optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
    iosX64 {
        compilations.all() {
            compilerOptions.options.apply {
                optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
}
