plugins {
    id("org.jetbrains.kotlin.multiplatform")
}


kotlin {
    wasmJs {
        binaries.executable()
        browser {
        }
    }
}