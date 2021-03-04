rootProject.name = "weather-function"

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        maven("https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
    }

    // This way we can map plugin to standard maven artifact
    // for maven repositories without plugin descriptor
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "konan") {
                val kotlinNativeVer = "0.7.1"
                useModule("org.jetbrains.kotlin:kotlin-native-gradle-plugin:$kotlinNativeVer")
            }
        }
    }
}
