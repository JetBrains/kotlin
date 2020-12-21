rootProject.name = "binary-compatibility-validator"


pluginManagement {
    resolutionStrategy {
        val kotlinVersion: String by settings
        val pluginPublishVersion: String by settings
        eachPlugin {
            if (requested.id.namespace?.startsWith("org.jetbrains.kotlin") == true) {
                useVersion(kotlinVersion)
            }
            if (requested.id.id == "com.gradle.plugin-publish") {
                useVersion(pluginPublishVersion)
            }
        }
    }
}
