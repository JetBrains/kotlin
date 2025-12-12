plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js(IR) {
        useEsModules()
        binaries.executable()
        nodejs()
    }
}