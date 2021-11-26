pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        val kotlin_version: String by settings
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.multiplatform" -> useVersion(kotlin_version)
            }
        }
    }
}

include(":shared")
include(":lib")