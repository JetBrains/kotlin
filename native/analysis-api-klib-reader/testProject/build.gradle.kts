plugins {
    kotlin("multiplatform")
    id("java-instrumentation")
}

kotlin {
    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()
    mingwX64()
}
