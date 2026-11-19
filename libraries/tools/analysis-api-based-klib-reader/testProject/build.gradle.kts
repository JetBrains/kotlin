plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("multiplatform")
}

kotlin {
    macosArm64()
    linuxX64()
    linuxArm64()
    mingwX64()
}
