plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {
    jvm()
    linuxX64()
}

group = "test"
version = "1.0"

publishing {
    repositories {
        maven("<localRepo>")
    }
}