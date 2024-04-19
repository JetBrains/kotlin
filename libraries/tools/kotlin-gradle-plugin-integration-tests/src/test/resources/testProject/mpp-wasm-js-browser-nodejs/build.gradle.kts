plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        browser {
        }
        nodejs {
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

with(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(rootProject)) {
    // canary nodejs that supports recent Wasm GC changes
    version = "21.0.0-v8-canary20231019bd785be450"
    downloadBaseUrl = "https://nodejs.org/download/v8-canary"
    npmInstallTaskProvider.configure {
        args += listOf("--network-concurrency", "1", "--mutex", "network")
        // It's required to pass compatibility checks in some NPM packages while using Node.js with Canary v8.
        // TODO remove as soon as we switch to release build of Node.js.
        args += listOf("--ignore-engines")
    }
}