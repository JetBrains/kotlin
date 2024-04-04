plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    mavenCentral()
}

with(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(rootProject)) {
    // canary nodejs that supports recent Wasm GC changes
    version = "21.0.0-v8-canary20231019bd785be450"
    downloadBaseUrl = "https://nodejs.org/download/v8-canary"
}

kotlin {

    wasmWasi {
        nodejs {}
        binaries.library()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}
