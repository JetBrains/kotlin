pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    val kotlin_version: String by settings
    plugins {
        kotlin("multiplatform").version(kotlin_version)
    }
}

include(":p1")
include(":p2")

