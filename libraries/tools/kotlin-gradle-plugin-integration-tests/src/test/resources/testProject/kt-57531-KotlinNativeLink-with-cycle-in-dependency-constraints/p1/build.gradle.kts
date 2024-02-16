import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.jetbrains.sample"
version = "1.0.0"

publishing {
    repositories {
        maven("<localRepo>")
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvm()
    linuxX64()
    linuxArm64()
    applyDefaultHierarchyTemplate()
}

dependencies {
    constraints {
        kotlin.sourceSets.all {
            val apiConfiguration = configurations.getByName(apiConfigurationName)
            apiConfiguration(project(":p2"))
        }
    }
}
