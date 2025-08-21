plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js {
        outputModuleName.set("@foo/bar")
        browser {
        }
        binaries.executable()
    }
}