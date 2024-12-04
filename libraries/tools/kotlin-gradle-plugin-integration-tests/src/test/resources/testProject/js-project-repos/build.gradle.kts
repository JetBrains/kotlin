import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    kotlin("js")
}

kotlin {
    js(IR) {
        binaries.executable()
        nodejs {
        }
    }
}

yarn

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().downloadBaseUrl = null
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().downloadBaseUrl = null
}