plugins {
    kotlin("js")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
        }
    }
}
