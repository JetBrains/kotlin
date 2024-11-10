plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.sample.kt72965"
version = 1.0

kotlin {
    js()
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}
