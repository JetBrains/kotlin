plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.jetbrains.kotlin.kar.test"
version = "1.0"

kotlin {
    linuxX64()

    sourceSets.linuxMain.dependencies {
        api("org.jetbrains.kotlin.kar.test:sample-linuxx64:1.0")
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}
