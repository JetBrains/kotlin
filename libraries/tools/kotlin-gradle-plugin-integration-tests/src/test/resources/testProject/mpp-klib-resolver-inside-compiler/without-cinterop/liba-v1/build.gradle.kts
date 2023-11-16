plugins {
    id("org.jetbrains.kotlin.multiplatform")
    `maven-publish`
}

group = "org.sample.kt-62515"
version = 1.0

repositories {
    mavenCentral()
    mavenLocal()
}

publishing {
    repositories {
        maven { url = uri("<localRepo>") }
    }
}

kotlin {
    linuxX64()
    linuxArm64()
}
