plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.jetbrains.kotlin.kar.test"
version = "2.0"

kotlin {
    linuxX64()
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}
