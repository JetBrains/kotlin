@file:OptIn(InternalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication

group = "test"
version = "1.0"

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    `maven-publish`
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

kotlin {
    val publication = kotlin.ext.get(
        KotlinTargetResourcesPublication.EXTENSION_NAME
    ) as KotlinTargetResourcesPublication

    listOf(
        androidTarget {
            publishAllLibraryVariants()
            compilations.all {
                kotlinOptions.jvmTarget = "1.8"
            }
        },
        jvm(),
        linuxX64(),
        wasmJs(),
    ).forEach { target ->
        publication.publishResourcesAsKotlinComponent(
            target,
            { sourceSet ->
                KotlinTargetResourcesPublication.ResourceRoot(
                    absolutePath = project.provider { project.file("src/${sourceSet.name}/multiplatformResources") },
                    includes = emptyList(),
                    excludes = emptyList()
                )
            },
            provider { File("embed/inside") }
        )
    }
}

publishing {
    repositories {
        maven("${buildDir}/repo")
    }
}

android {
    namespace = "test"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}