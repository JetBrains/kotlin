@file:OptIn(InternalKotlinGradlePluginApi::class)

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import java.io.File

group = "test"
version = "1.0"

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `maven-publish`
}

kotlin {
    val publication = if (<enablePublication>) kotlin.ext.get(
        KotlinTargetResourcesPublication.EXTENSION_NAME
    ) as KotlinTargetResourcesPublication else null

    listOf(
        androidTarget {
            publishAllLibraryVariants()
            compilations.all {
                kotlinOptions.jvmTarget = "1.8"
            }
        },
        jvm(),
        linuxX64(),
        iosArm64(),
        wasmJs(),
        wasmWasi(),
    ).forEach { target ->
        val relativeResourcePlacement = provider { File("embed/${project.name}") }
        val sourceSetPathProvider: (KotlinSourceSet) -> (Provider<File>) = { sourceSet ->
            project.provider { project.file("src/${sourceSet.name}/multiplatformResources") }
        }
        publication?.publishResourcesAsKotlinComponent(
            target = target,
            resourcePathForSourceSet = { sourceSet ->
                KotlinTargetResourcesPublication.ResourceRoot(
                    absolutePath = sourceSetPathProvider(sourceSet),
                    includes = emptyList(),
                    excludes = emptyList(),
                )
            },
            relativeResourcePlacement = relativeResourcePlacement,
        )
    }

    sourceSets.commonMain {
        dependencies {
            <dependencies>
        }
    }
}

android {
    namespace = "test.${project.name}"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}
