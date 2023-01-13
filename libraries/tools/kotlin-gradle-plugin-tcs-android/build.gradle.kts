plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly("com.android.tools.build:gradle:7.2.1")
    compileOnly(project(":kotlin-gradle-plugin"))
    compileOnly(project(":kotlin-gradle-plugin-idea"))
}

configureKotlinCompileTasksGradleCompatibility()

kotlin {
    sourceSets.all {
        languageSettings.optIn("org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi")
        /* Workaround for KT-54823 */
        languageSettings.optIn("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")
    }
}

/* This module is just for local development / prototyping and demos */
if (!kotlinBuildProperties.isTeamcityBuild) {
    publish()
    standardPublicJars()
}
