plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly("com.android.tools.build:gradle:7.0.0")
    compileOnly(project(":kotlin-gradle-plugin"))
}

configureKotlinCompileTasksGradleCompatibility()

kotlin {
    sourceSets.all {
        languageSettings.optIn("org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi")
    }
}

/* This module is just for local development / prototyping and demos */
if (!kotlinBuildProperties.isTeamcityBuild) {
    publish()
    standardPublicJars()
}
