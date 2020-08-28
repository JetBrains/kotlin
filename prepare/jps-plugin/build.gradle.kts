description = "Kotlin JPS plugin"

plugins {
    java
}

val compilerComponents = rootProject.extra["compilerModulesForJps"] as List<String>

val projectsToShadow = compilerComponents + listOf(":jps-plugin")

dependencies {
    projectsToShadow.forEach {
        embedded(project(it)) { isTransitive = false }
    }

    embedded(projectRuntimeJar(":kotlin-daemon-client"))
}

runtimeJar {
    manifest.attributes["Main-Class"] = "org.jetbrains.kotlin.runner.Main"
    manifest.attributes["Class-Path"] = "kotlin-stdlib.jar"
    from(files("$rootDir/resources/kotlinManifest.properties"))
}
