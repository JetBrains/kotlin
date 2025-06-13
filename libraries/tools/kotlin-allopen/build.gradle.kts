plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-model"))
}

gradlePlugin {
    plugins {
        create("kotlinAllopenPlugin") {
            id = "org.jetbrains.kotlin.plugin.allopen"
            displayName = "Kotlin All Open compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.allopen.gradle.AllOpenGradleSubplugin"
        }
        create("kotlinSpringPlugin") {
            id = "org.jetbrains.kotlin.plugin.spring"
            displayName = "Kotlin Spring compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.allopen.gradle.SpringGradleSubplugin"
        }
    }
}

extra["oldCompilerForGradleCompatibility"] = true
