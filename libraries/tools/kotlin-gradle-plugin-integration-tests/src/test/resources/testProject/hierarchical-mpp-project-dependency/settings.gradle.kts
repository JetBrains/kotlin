pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "mpp-granular-metadata-demo"

include("my-lib-foo", "my-lib-bar", "my-app")

enableFeaturePreview("GRADLE_METADATA")