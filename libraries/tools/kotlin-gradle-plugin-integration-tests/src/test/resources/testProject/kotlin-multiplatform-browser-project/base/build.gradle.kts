plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        useCommonJs()
        browser()
    }
}