plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    js {
        binaries.executable()
        nodejs()
        useEsModules()
    }
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().version.set("22.2.0")
}