plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(libs.android.gradle.plugin.gradle)
    compileOnly(project(":kotlin-gradle-plugin")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-common")
        }
    }
    compileOnly(project(":kotlin-gradle-plugin-api")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-api-common")
        }
    }
    compileOnly(project(":kotlin-gradle-plugin-idea"))
}

configureKotlinCompileTasksGradleCompatibility()

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
