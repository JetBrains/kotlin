plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()
    sourceSets {
        commonMain {
            dependencies {
                api(project(":lib"))
            }
        }
    }
}