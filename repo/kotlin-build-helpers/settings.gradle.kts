pluginManagement {
    apply(from = "../gradle-settings-conventions/cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts")
    apply(from = "../gradle-settings-conventions/kotlin-bootstrap/src/main/kotlin/kotlin-bootstrap.settings.gradle.kts")

    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kotlin-build-helpers"
