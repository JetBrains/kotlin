plugins {
    kotlin("multiplatform")
}

kotlin {
    macosArm64()
    @Suppress("DEPRECATION")
    macosX64()
    linuxX64()
    linuxArm64()
    mingwX64()
}
