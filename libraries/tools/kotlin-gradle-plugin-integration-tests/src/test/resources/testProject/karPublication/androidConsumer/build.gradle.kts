plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvm()
    android {
        namespace = "org.jetbrains.kotlin.kar.test.consumer"
        compileSdk = 31
        minSdk = 23
    }

    sourceSets.commonMain.dependencies {
        implementation("org.jetbrains.kotlin.kar.test:sample:1.0")
    }
}
