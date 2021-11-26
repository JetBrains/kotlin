plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}
