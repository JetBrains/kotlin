plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    `maven-publish`
}

group = "org.jetbrains.kotlin.kar.test"
version = "1.0"

kotlin {
    jvm()
    android {
        namespace = "org.jetbrains.kotlin.kar.test.producer"
        compileSdk = 31
        minSdk = 23
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}
