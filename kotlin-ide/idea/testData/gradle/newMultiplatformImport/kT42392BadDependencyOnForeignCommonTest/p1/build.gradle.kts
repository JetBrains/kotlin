plugins {
    id("com.android.library")
    kotlin("multiplatform")
}
android {
    compileSdkVersion(26)
}
kotlin {
    jvm()
    android()
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(project(":p2"))
            }
        }
    }
}
