plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.jetbrains.kotlin.kar.test"
version = "1.0"

kotlin {
    linuxX64()
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}
