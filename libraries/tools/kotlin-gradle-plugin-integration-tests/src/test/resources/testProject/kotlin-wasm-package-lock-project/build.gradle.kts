plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        useCommonJs()
        binaries.executable()
        nodejs {
        }
    }
}