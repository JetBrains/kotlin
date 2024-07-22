plugins {
    kotlin("multiplatform")
}

version = "1.0.5"

kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}