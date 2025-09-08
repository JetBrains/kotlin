plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(libs.android.gradle.plugin.gradle)
    compileOnly(project(":kotlin-gradle-plugin"))
    compileOnly(project(":kotlin-gradle-plugin-api"))
    compileOnly(project(":kotlin-gradle-plugin-idea"))
}

configureKotlinCompileTasksGradleCompatibility()
configureJvmToolchain(JdkMajorVersion.JDK_11_0)

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi")
        /* Workaround for KT-54823 */
        optIn.add("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")
    }
}

/* This module is just for local development / prototyping and demos */
if (!kotlinBuildProperties.isTeamcityBuild) {
    publish()
    standardPublicJars()
}
