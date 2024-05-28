plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "test"
version = "1.0"

publishing {
    repositories {
        maven("<localRepo>")
    }
}

kotlin {
    withSourcesJar(publish = true)
    jvm()
    linuxX64()
}