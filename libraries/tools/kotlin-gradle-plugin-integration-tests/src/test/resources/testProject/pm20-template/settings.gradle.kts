pluginManagement {
    fun RepositoryHandler.configureRepositories() {
        mavenLocal()
        mavenCentral()
        if (this === pluginManagement.repositories) {
            gradlePluginPortal()
        } else {
            maven("$rootDir/build/repo")
        }
    }

    repositories {
        configureRepositories()
    }
    plugins {
        val kotlin_version: String by settings
        kotlin("multiplatform.pm20").version(kotlin_version)
        kotlin("multiplatform").version(kotlin_version)
    }
    dependencyResolutionManagement.repositories {
        configureRepositories()
    }
}