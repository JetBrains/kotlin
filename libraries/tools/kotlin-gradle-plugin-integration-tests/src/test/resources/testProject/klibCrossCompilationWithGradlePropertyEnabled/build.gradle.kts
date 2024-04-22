plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {
    iosArm64() {
        binaries.executable()
    }
}

group = "org.jetbrains.kotlin"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}
