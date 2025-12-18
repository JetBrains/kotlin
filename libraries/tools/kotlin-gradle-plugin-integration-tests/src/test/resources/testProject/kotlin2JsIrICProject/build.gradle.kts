plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0"


repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js(IR) {
        binaries.executable()
        nodejs {}
    }
}
