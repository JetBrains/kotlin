group = "com.example"
version = "1.0"

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    linuxX64("host")
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}
