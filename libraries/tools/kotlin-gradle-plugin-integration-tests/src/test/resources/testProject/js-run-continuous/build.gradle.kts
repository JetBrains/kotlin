plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        binaries.executable()
        browser {
            commonWebpackConfig {
                // don't auto-open the browser, it's annoying
                devServer?.open = false
            }
        }
    }
}
