plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    `maven-publish`
}

group = "app"
version = "1.0"

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

        withAndroidTestOnJvm()
        sourceSets.getByName("androidTestOnJvm").dependencies {
            implementation("junit:junit:4.13.2")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":lib"))
            }
        }
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}