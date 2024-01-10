plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
}

gradlePlugin {
    plugins {
        create("jso") {
            id = "org.jetbrains.kotlin.plugin.jso"
            displayName = "Kotlin compiler plugin for kotlinx.jso library"
            description = displayName
            implementationClass = "org.jetbrains.kotlinx.jso.gradle.JsoKotlinGradleSubplugin"
        }
    }
}