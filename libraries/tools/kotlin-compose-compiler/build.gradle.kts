plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-model"))

    commonCompileOnly(project(":kotlin-compiler-embeddable"))
}

gradlePlugin {
    plugins {
        create("kotlinComposeCompilerPlugin") {
            id = "org.jetbrains.kotlin.plugin.compose-compiler-gradle-plugin"
            displayName = "Kotlin Compose Compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradleSubplugin"
        }
    }
}
