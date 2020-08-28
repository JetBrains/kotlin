pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    plugins {
        val kotlin_version: String by settings
        kotlin("multiplatform").version(kotlin_version)
    }
}

rootProject.name = "lib-and-app"

include("lib")
include("libtests")