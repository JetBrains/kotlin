pluginManagement {
    resolutionStrategy {
        eachPlugin{
            when (requested.id.id) {
                "kotlin-dce-js" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:<pluginMarkerVersion>")
            }
        }
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "kotlin-js-plugin"