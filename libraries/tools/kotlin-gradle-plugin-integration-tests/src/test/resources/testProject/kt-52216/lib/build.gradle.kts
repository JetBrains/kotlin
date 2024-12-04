plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "kt52216"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    js()
    linuxX64()
}


publishing {
    repositories {
        maven("<localRepo>")
    }
}
