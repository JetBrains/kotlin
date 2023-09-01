plugins {
    kotlin("multiplatform")
    `maven-publish`
}

repositories {
    mavenLocal()
    maven("../repo")
    mavenCentral()
}

group = "com.example"
version = "1.0"

kotlin {
    jvm()
    linuxX64()
}

publishing {
    repositories {
        maven("../repo")
    }
}
