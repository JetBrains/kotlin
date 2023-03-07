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

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

android {
    compileSdkVersion(30)
    namespace = "com.example.producer"
}

kotlin {
    jvm()
    android { publishAllLibraryVariants() }
}
