dependencyResolutionManagement {
    repositories {
        mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kotlin-build-helpers"
