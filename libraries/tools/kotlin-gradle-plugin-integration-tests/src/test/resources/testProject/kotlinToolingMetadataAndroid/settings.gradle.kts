pluginManagement {
    repositories {
        mavenLocal()
        google()
        gradlePluginPortal()
    }
    val kotlin_version: String by settings
    val android_tools_version: String by settings
    plugins {
        kotlin("multiplatform").version(kotlin_version)
        kotlin("android").version(kotlin_version)
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("com.android")) {
                useModule("com.android.tools.build:gradle:$android_tools_version")
            }
        }
    }
}

rootProject.name = "project"