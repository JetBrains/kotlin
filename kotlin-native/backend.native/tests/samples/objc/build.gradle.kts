plugins {
    kotlin("multiplatform")
}

kotlin {
    macosX64("objc") {
        binaries {
            // Muted, see https://youtrack.jetbrains.com/issue/KT-75598.
            // executable {
            //     entryPoint = "sample.objc.main"
            // }
        }
    }
}