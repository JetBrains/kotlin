plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

android {
    compileSdkVersion(26)
}

kotlin {
    android()
    jvm()
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(project(":p3"))
            }
        }
    }
}
