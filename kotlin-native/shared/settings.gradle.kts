pluginManagement {
    val rootProperties = java.util.Properties().apply {
        rootDir.resolve("../gradle.properties").reader().use(::load)
    }

    val kotlinVersion: String by rootProperties

    repositories {
        maven(project.bootstrapKotlinRepo)
        maven("https://cache-redirector.jetbrains.com/maven-central")
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
            }
        }
    }
}

rootProject.name = "kotlin-native-shared"
