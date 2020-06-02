pluginManagement {
    val rootProperties = java.util.Properties().apply {
        rootDir.resolve("../gradle.properties").reader().use(::load)
    }

    val buildKotlinCompilerRepo: String by rootProperties
    val buildKotlinVersion: String by rootProperties

    repositories {
        maven(buildKotlinCompilerRepo)
        maven("https://cache-redirector.jetbrains.com/maven-central")
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$buildKotlinVersion")
            }
        }
    }
}

rootProject.name = "kotlin-native-shared"