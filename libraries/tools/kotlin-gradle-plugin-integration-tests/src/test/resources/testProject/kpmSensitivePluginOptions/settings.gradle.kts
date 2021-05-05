pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    plugins {
        kotlin("multiplatform.pm20").version("1.5.255-SNAPSHOT")
    }
}

rootProject.name = "kpmSensitivePluginOptions"