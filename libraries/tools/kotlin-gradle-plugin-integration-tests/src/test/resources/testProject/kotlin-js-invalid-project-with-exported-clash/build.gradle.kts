plugins {
    kotlin("js")
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