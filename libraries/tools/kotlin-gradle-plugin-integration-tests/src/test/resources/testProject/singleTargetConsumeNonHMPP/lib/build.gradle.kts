plugins {
    kotlin("multiplatform") version "1.6.10"
    `maven-publish`
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("${rootProject.projectDir}/../repo")
    }
}

kotlin {
    jvm()
    linuxX64()
    linuxArm64()
}

group = "test"
version = "1.0"

publishing {
    repositories {
        maven("${rootProject.projectDir}/../repo")
    }
}