plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        commonTest.dependencies {
            api(project(":p2"))
        }
    }
}