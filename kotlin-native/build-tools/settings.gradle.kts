pluginManagement {
    val rootProperties = java.util.Properties().apply {
        rootDir.resolve("../gradle.properties").reader().use(::load)
    }

    repositories {
        maven("https://cache-redirector.jetbrains.com/maven-central")
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
            }
        }
    }
}

rootProject.name = "kotlin-native-build-tools"

includeBuild("../shared")
