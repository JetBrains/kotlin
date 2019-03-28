pluginManagement {
    repositories {
        jcenter()
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    }

    resolutionStrategy {
        // Workaround to be able to read plugin version from the properties file.
        // See: https://stackoverflow.com/questions/52800536/how-to-use-plugin-version-from-gradle-properties-in-gradle-kotlin-dsl.
        val kotlinVersion = getRootProperties().getValue("kotlin_version") as String
        eachPlugin {
            when {
                requested.id.id == "org.jetbrains.kotlin.native.cocoapods" ||
                requested.id.id == "kotlin-native-cocoapods" ->
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                requested.id.id.startsWith("org.jetbrains.kotlin") ->
                    useVersion(kotlinVersion)
            }
        }
    }
}

// Reuse Kotlin version and other properties from the root samples project.
fun getRootProperties() = java.util.Properties().apply {
    val rootProjectGradlePropertiesFile = file("${rootProject.projectDir}/../../gradle.properties")
    if (!rootProjectGradlePropertiesFile.isFile) {
        throw Exception("File $rootProjectGradlePropertiesFile does not exist or is not a file")
    }

    rootProjectGradlePropertiesFile.inputStream().use { inputStream ->
        load(inputStream)
    }
}

gradle.beforeProject {
    getRootProperties().forEach { key, value ->
        if (!project.hasProperty(key as String))
            project.extra[key] = value
    }
}
