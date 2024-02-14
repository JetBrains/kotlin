@file:OptIn(InternalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication

group = "test"
version = "1.0"

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {
    val publication = kotlin.ext.get(
        KotlinTargetResourcesPublication.EXTENSION_NAME
    ) as KotlinTargetResourcesPublication

    listOf(
        jvm(),
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
