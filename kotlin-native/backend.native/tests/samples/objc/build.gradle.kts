plugins {
    kotlin("multiplatform")
}

kotlin {
    macosArm64("objc") {
        binaries {
            executable {
                entryPoint = "sample.objc.main"
            }
        }
    }
}