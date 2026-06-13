@file:OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

group = "org.jetbrains.kotlin"

kotlin {
    jvmToolchain(17)

    coreLibrariesVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()

    compilerOptions {
        compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
    }
}

repositories {
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
}


dependencies {
    testFixturesImplementation(libs.jgit)
}
