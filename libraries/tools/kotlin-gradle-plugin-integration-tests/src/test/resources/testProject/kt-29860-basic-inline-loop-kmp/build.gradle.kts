plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js(IR) {
        nodejs()
    }
}
