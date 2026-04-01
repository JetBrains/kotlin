plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        nodejs {
        }
        binaries.executable()
    }
}
