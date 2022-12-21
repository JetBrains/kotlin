plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js {
        moduleName = "@foo/bar"
        browser {
        }
        binaries.executable()
    }
}