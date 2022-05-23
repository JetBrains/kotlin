description = "Kotlin lombok compiler plugin"

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-lombok-compiler-plugin")) { isTransitive = false }

    commonApi(project(":kotlin-gradle-plugin-model"))
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
