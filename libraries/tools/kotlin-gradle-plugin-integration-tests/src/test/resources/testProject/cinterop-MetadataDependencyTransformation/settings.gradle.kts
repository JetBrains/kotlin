pluginManagement {
    val kotlin_version: String? by settings
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }

    plugins {
        kotlin("multiplatform") version (kotlin_version ?: "1.6.255-SNAPSHOT")
    }
}

rootProject.name = "kotlin-multiplatform-projects"
include(":p1")
include(":p2")
include(":p3")
