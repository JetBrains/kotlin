description = "Kotlin JPS plugin"

plugins {
    java
    id("pill-configurable")
}

val projectsToShadow = listOf(
    ":core:type-system",
    ":kotlin-build-common",
    ":compiler:cli-common",
    ":kotlin-compiler-runner",
    ":daemon-common",
    ":daemon-common-new",
    ":core:descriptors",
    ":core:descriptors.jvm",
    ":idea:idea-jps-common",
    ":jps-plugin",
    ":kotlin-preloader",
    ":compiler:util",
    ":core:util.runtime"
)

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
