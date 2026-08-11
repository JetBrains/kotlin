pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        // Published producer must win over any remote with the same GAV.
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "ktij-39840-consumer"

include(":android-lib")
include(":kmp-control")
