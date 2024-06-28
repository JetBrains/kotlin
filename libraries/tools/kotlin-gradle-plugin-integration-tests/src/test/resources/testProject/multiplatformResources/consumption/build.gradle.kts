@file:OptIn(ComposeKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication

group = "test"
version = "1.0"

plugins {
    kotlin("multiplatform")
    id("com.android.library") apply false
}

kotlin {
    val publication = project.ext.get(
        KotlinTargetResourcesPublication.EXTENSION_NAME
    ) as KotlinTargetResourcesPublication

    listOf(
        linuxX64(),
        iosArm64(),
        iosSimulatorArm64(),
        wasmJs(),
        wasmWasi(),
        js(),
    ).forEach { target ->
        val assemblyTask = publication.resolveResources(target)
        tasks.register<Copy>("${target.disambiguationClassifier}ResolveResources") {
            from(assemblyTask)
            into(layout.buildDirectory.dir("${target.disambiguationClassifier}ResolvedResources"))
        }
    }

    sourceSets.commonMain {
        dependencies {
            <dependencies>
        }
    }
}