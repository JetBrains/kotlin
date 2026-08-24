plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.jetbrains.kotlin.kar.test"
version = "1.0"

kotlin {
    jvm()
    js {
        nodejs()
    }
    linuxX64()
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}
