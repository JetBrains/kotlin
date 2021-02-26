pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

enableFeaturePreview("GRADLE_METADATA")

include(":app")
include(":lib")