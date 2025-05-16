plugins {
    kotlin("multiplatform")
}

kotlin {
    iosArm64() {
        binaries.executable()
    }
}
repositories {
    mavenLocal()
    mavenCentral()
}
