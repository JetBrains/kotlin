plugins {
    kotlin("js")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    @Suppress("DEPRECATION")
    js(BOTH) {
        nodejs {
        }
    }
}