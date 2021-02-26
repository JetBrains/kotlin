pluginManagement {
    val rootProperties = java.util.Properties().apply {
        rootDir.resolve("../gradle.properties").reader().use(::load)
    }

    val buildKotlinCompilerRepo: String by rootProperties
    val kotlinCompilerRepo: String by rootProperties
    val buildKotlinVersion by rootProperties

    repositories {
        maven(kotlinCompilerRepo)
        maven(buildKotlinCompilerRepo)
        maven("https://cache-redirector.jetbrains.com/maven-central")
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$buildKotlinVersion")
            }
        }
    }
}

rootProject.name = "kotlin-native-build-tools"

includeBuild("../shared")
