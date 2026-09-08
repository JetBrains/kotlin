import gradle.addKgpGradleApiDependency

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    compileOnly(libs.android.gradle.plugin.gradle)
    compileOnly(project(":kotlin-gradle-plugin"))
    compileOnly(project(":kotlin-gradle-plugin-api"))
    compileOnly(project(":kotlin-gradle-plugin-idea"))
    addKgpGradleApiDependency("compileOnly")
    compileOnly(kotlin("stdlib"))
}

configureKotlinCompileTasksGradleCompatibility()
jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_11_0
    targetBytecodeVersion = JdkMajorVersion.JDK_11_0
}

kotlin {
    coreLibrariesVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi")
        /* Workaround for KT-54823 */
        optIn.add("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")
    }
}

/* This module is just for local development / prototyping and demos */
if (!kotlinBuildProperties.isTeamcityBuild.get()) {
    publish()
    standardPublicJars()
}
