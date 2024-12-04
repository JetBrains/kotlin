plugins {
    kotlin("js")
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
