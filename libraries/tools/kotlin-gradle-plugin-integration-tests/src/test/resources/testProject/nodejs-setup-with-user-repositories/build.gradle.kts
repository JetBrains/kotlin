plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    js {
        nodejs()
    }
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
}