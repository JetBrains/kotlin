import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    google { setUrl("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2") }
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    gradlePluginPortal()
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
    jvmToolchain(17)
}

/**
 * Don't add new dependencies here
 */
dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    api(libs.jetbrains.ideaExt.gradlePlugin)
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.guava" && requested.name == "guava") {
            useVersion("32.0.1-jre")
            because("CVE-2023-2976, CVE-2020-8908")
        }
    }
}
