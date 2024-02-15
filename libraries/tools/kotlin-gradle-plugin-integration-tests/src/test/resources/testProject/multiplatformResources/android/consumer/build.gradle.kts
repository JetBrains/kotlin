@file:OptIn(ComposeKotlinGradlePluginApi::class)

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import java.io.File

group = "test"
version = "1.0"

plugins {
    kotlin("multiplatform")
    id("com.android.application")
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":projectDependency"))
                api("test:publication:+")
            }
        }
    }

    val publication = project.ext.get(
        KotlinTargetResourcesPublication.EXTENSION_NAME
    ) as KotlinTargetResourcesPublication

    listOf(
        androidTarget {
            publishAllLibraryVariants()
            compilations.all {
                kotlinOptions.jvmTarget = "1.8"
            }
        }
    ).forEach { target ->
        val fontsFilter = if (target is KotlinAndroidTarget) listOf("fonts/*") else emptyList()
        val relativeResourcePlacement = provider { File("embed/self") }
        val sourceSetPathProvider: (KotlinSourceSet) -> (Provider<File>) = { sourceSet ->
            project.provider { project.file("src/${sourceSet.name}/multiplatformResources") }
        }

        publication.publishResourcesAsKotlinComponent(
            target = target,
            resourcePathForSourceSet = { sourceSet ->
                KotlinTargetResourcesPublication.ResourceRoot(
                    resourcesBaseDirectory = sourceSetPathProvider(sourceSet),
                    includes = emptyList(),
                    excludes = fontsFilter,
                )
            },
            relativeResourcePlacement = relativeResourcePlacement,
        )
        if (target is KotlinAndroidTarget) {
            publication.publishInAndroidAssets(
                target = target,
                resourcePathForSourceSet = { sourceSet ->
                    KotlinTargetResourcesPublication.ResourceRoot(
                        resourcesBaseDirectory = sourceSetPathProvider(sourceSet),
                        includes = fontsFilter,
                        excludes = emptyList(),
                    )
                },
                relativeResourcePlacement = relativeResourcePlacement,
            )
        }
    }
}

android {
    namespace = "test.consumer"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}