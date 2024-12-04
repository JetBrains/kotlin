plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "kt50925"
version = "1.0"

kotlin {
    linuxX64()
    linuxArm64()
}

publishing {
    repositories {
        maven("<localRepo>") {
            name = "LocalRepo"
        }
    }
}
