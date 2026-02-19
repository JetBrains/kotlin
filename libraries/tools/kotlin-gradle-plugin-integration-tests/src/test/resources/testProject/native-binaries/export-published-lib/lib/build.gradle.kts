plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.example"
version = "1.0"

kotlin {
    jvm()
    linuxX64()
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}
