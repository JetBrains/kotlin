plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))

    commonCompileOnly(project(":kotlin-gradle-plugin"))
    commonCompileOnly(project(":kotlin-compiler-embeddable"))
}

gradlePlugin {
    plugins {
        create("kotlinSerialization") {
            id = "org.jetbrains.kotlin.plugin.serialization"
            displayName = "Kotlin compiler plugin for kotlinx.serialization library"
            description = displayName
            implementationClass = "org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin"
        }
    }
}
