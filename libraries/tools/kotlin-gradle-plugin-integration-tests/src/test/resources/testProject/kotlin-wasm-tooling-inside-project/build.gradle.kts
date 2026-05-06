plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

version = "1.0.0-SNAPSHOT"

kotlin {
    wasmJs {
        binaries.executable()
        browser {
        }
    }
}
