plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.sample.kt-73511"
version = 1.0

publishing {
    repositories {
        maven { url = uri("<localRepo>") }
    }
}

repositories {
    mavenCentral()
}

kotlin {
    macosArm64()
}
