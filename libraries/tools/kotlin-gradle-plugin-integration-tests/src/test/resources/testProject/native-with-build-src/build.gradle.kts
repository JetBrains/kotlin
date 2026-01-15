plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    macosArm64 {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
}