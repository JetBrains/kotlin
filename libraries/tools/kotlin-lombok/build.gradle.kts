description = "Kotlin lombok compiler plugin"

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-model"))

    embedded(project(":kotlin-lombok-compiler-plugin")) { isTransitive = false }
}

projectTest(parallel = true)

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
