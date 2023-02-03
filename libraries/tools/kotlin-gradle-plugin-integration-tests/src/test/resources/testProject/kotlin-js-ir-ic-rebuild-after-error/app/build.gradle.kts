plugins {
    kotlin("js")
}

kotlin {
    js {
        nodejs {
        }
        binaries.executable()
    }
}
