plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

kotlin {
    linuxX64()
    linuxArm64()

    androidLibrary {
        compileSdk = 33
        namespace = "org.jetbrains.sample"
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }

    publications {
        val android by getting {
            this as MavenPublication
            artifactId = "tcs-android"
            version = "2.0"
            groupId = "sample"
        }
    }
}