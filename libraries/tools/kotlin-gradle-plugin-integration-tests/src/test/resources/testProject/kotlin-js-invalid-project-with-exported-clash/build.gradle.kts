plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        useEsModules()
        binaries.executable()
        nodejs()
    }
}