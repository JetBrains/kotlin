plugins {
    kotlin("multiplatform")
    id("common-configuration")
}

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlinStdlib())
            }
        }
    }
}
