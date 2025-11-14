description = "Kotlin lombok compiler plugin"

plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
}

gradlePlugin {
    plugins {
        create("kotlinLombokPlugin") {
            id = "org.jetbrains.kotlin.plugin.lombok"
            displayName = "Kotlin Lombok plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.lombok.gradle.LombokSubplugin"
        }
    }
}
