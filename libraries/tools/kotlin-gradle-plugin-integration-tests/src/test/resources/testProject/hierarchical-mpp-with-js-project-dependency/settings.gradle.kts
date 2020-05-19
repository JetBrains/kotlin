pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "hierarchical-mpp-with-js"

include("my-lib-foo", "my-app")

enableFeaturePreview("GRADLE_METADATA")
