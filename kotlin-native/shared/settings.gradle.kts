pluginManagement {
    val rootProperties = java.util.Properties().apply {
        rootDir.resolve("../gradle.properties").reader().use(::load)
    }

    val kotlinCompilerRepo: String by rootProperties
    val kotlinVersion: String by rootProperties

    repositories {
        maven(kotlinCompilerRepo)
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
