rootProject.name = "weather-function"

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
    }

    // This way we can map plugin to standard maven artifact
    // for maven repositories without plugin descriptor
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "konan") {
                val kotlinNativeVer = "1.3.41"
                useModule("org.jetbrains.kotlin:kotlin-native-gradle-plugin:$kotlinNativeVer")
            }
        }
    }
}
