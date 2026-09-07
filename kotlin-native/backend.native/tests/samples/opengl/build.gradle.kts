plugins {
    kotlin("multiplatform")
}

kotlin {
    macosArm64("opengl") {
        binaries {
            executable {
                entryPoint = "sample.opengl.main"
            }
        }
    }
}