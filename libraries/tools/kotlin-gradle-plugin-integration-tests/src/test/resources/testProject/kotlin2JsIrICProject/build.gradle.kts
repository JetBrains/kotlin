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
    js {
        binaries.executable()
        nodejs {}
    }
}
