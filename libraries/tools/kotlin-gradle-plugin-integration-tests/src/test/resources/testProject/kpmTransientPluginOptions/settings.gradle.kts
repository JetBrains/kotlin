pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    plugins {
        val kotlin_version: String by settings
        kotlin("multiplatform.pm20").version(kotlin_version)
    }
}

rootProject.name = "kpmTransientPluginOptions"