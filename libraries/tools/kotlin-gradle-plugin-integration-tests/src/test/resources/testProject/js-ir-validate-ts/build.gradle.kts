plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js(IR) {
        binaries.library()
        browser()
    }
}
