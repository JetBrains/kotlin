plugins {
    kotlin("multiplatform")
}

kotlin {
    <SingleNativeTarget>("native") {
        binaries.executable()
    }
}
