pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.name == "multiplatform") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0-dev-7568") // TODO: use the Gradle plugin from the current build
            }
        }
    }

    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-dev") // TODO: use the Gradle plugin from the current build
        gradlePluginPortal()
    }
}

rootProject.name = "test"
