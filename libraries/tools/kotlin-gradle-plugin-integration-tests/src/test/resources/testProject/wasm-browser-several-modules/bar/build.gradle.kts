plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        binaries.executable()
        browser {
        }
    }
}