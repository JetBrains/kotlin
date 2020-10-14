pluginManagement {
    repositories {
        mavenLocal()
        jcenter()
        gradlePluginPortal()
    }
}

enableFeaturePreview("GRADLE_METADATA")

include(":app")
include(":lib")