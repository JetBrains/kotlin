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
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().downloadBaseUrl.set(null as String?)
}