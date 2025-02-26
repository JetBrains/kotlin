plugins {
    kotlin("js")
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
        }
        generateTypeScriptDefinitions()
    }
}
