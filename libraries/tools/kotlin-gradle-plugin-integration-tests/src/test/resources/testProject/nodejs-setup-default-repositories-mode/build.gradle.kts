plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        browser()
        nodejs()
    }
}
