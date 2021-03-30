plugins {
    kotlin("multiplatform")
}

kotlin {
    macosX64("objc") {
        binaries {
            executable {
                entryPoint = "sample.objc.main"
            }
        }
    }
}