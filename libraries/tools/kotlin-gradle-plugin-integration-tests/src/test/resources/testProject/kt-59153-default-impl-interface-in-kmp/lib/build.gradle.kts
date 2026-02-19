plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js() {
        nodejs()
    }
}
