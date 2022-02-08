plugins {
    kotlin("multiplatform")
}

kotlin {
    macosX64("opengl") {
        binaries {
            executable {
                entryPoint = "sample.opengl.main"
            }
        }
    }
}