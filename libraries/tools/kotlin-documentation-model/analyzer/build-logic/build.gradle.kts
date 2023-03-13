import java.util.*

plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

// TODO define versions in Gradle Version Catalog https://github.com/Kotlin/dokka/pull/2884
val properties = file("../gradle.properties").inputStream().use {
    Properties().apply { load(it) }
}

val kotlinVersion = properties["kotlin_version"]

dependencies {
    // Import Gradle Plugins that will be used in the buildSrc pre-compiled script plugins, and any `build.gradle.kts`
    // files in the project.
    // Use their Maven coordinates (plus versions), not Gradle plugin IDs!
    // This should be the only place that Gradle plugin versions are defined, so they are aligned across all build scripts

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.12.1")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")
}
