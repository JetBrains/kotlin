import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

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

    sourceSets.commonMain.get().dependencies {
        api(project(":p1"))
    }
}
