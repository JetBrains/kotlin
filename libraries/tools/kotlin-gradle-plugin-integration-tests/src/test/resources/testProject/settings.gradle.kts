
pluginManagement {
    apply(from = "gradle/cache-redirector.settings.gradle.kts")

    repositories {
        mavenCentral()
        mavenLocal()
        google()
        gradlePluginPortal()
    }
}

rootProject.name = "liba"
