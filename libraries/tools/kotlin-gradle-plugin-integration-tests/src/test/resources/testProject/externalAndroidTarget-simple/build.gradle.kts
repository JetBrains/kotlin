plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

kotlin {
    linuxX64()
    linuxArm64()

    androidExperimental {
        compileSdk = 33
        namespace = "org.jetbrains.sample"

        withAndroidTestOnJvm()
        sourceSets.getByName("androidTestOnJvm").dependencies {
            implementation("junit:junit:4.13.2")
        }
    }
}
