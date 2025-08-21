plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        outputModuleName.set("redefined-wasm-module-name")
        <JsEngine> {
        }
        binaries.executable()
    }
    js {
        outputModuleName.set("redefined-js-module-name")
        browser {
        }
        binaries.executable()
    }
}