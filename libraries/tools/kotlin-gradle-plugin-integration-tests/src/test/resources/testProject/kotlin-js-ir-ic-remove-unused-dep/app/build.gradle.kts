plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        browser {}
        binaries.executable()
    }
}
