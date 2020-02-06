rootProject.name = "binary-compatibility-validator"

val kotlinVersion: String by settings

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace?.startsWith("org.jetbrains.kotlin") == true) {
                useVersion(kotlinVersion)
            }
        }
    }
}
