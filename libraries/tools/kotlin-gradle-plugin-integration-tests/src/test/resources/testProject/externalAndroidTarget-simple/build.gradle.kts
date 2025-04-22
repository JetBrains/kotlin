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

    androidLibrary {
        compileSdk = 33
        namespace = "org.jetbrains.sample"

        <host-test-dsl>
    }

    val androidTestSourceSetName = "<host-test-source-set-name>"
    sourceSets.getByName(androidTestSourceSetName).dependencies {
        implementation("junit:junit:4.13.2")
    }
}
