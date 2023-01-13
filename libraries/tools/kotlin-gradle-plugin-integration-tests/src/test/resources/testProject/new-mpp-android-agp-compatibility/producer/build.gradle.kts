val kotlin_version: String by extra
plugins {
    `maven-publish`
    id("com.android.library")
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0.0-SNAPSHOT"


publishing {
    repositories {
        maven("<localRepo>") {
            name = "buildDir"
        }
    }
}

android {
    compileSdkVersion(30)
}

kotlin {
    jvm()
    android { publishAllLibraryVariants() }
}
