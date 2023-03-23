import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.jetbrains.sample"
version = "1.0.0"

publishing {
    repositories {
        maven(rootProject.buildDir.resolve("repo"))
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvm()
    linuxX64()
    linuxArm64()
    targetHierarchy.default()

    sourceSets.commonMain.get().dependencies {
        api(project(":p1"))
    }
}
