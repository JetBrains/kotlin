/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.mpp.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import java.io.File

data class MultiplatformResourcesITAndroidConfiguration(
    val version: String,
    val pluginId: String,
) : java.io.Serializable

fun KGPBaseTest.resourcesProducerProject(
    gradleVersion: GradleVersion,
    providedJdk: JdkVersions.ProvidedJdk? = null,
    androidVersion: String? = null,
) = project(
    "multiplatformResources/publication",
    gradleVersion,
    buildJdk = providedJdk?.location,
    buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
) {
    addKgpToBuildScriptCompilationClasspath()
    androidVersion?.let { addAgpToBuildScriptCompilationClasspath(it) }
    configureStandardResourcesProducer(
        androidVersion?.let {
            MultiplatformResourcesITAndroidConfiguration(
                it,
                "com.android.library",
            )
        }
    )
}

fun TestProject.configureStandardResourcesProducer(
    androidConfiguration: MultiplatformResourcesITAndroidConfiguration?,
    relativeResourcePlacement: Project.() -> Provider<File> = {
        project.provider {
            File("embed/${project.name}")
        }
    }
) {
    buildScriptInjection {
        if (androidConfiguration != null) {
            project.plugins.apply(androidConfiguration.pluginId)
        }
        project.plugins.apply("maven-publish")

        project.group = "test"
        project.version = "1.0"

        project.applyMultiplatform {
            configureStandardResourcesProducerTargets(withAndroid = androidConfiguration != null)
            configureStandardResourcesAndAssetsPublication(relativeResourcePlacement)
        }

        publishing.repositories.maven {
            it.url = project.uri(project.layout.projectDirectory.dir("repo"))
        }
    }
}

fun GradleProjectBuildScriptInjectionContext.configureStandardResourcesProducerTargets(withAndroid: Boolean) {
    with(kotlinMultiplatform) {
        if (withAndroid) {
            androidTarget { -> Unit
                publishAllLibraryVariants()
                compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
            }
        }
        jvm()
        linuxX64()
        iosArm64()
        iosSimulatorArm64()
        wasmJs()
        wasmWasi()
        js()
    }

    if (withAndroid) {
        with(androidBase) {
            namespace = "test.${project.name}"
            compileSdk = 34
            defaultConfig {
                minSdk = 24
            }
        }
    }
}

fun KotlinMultiplatformExtension.configureStandardResourcesAndAssetsPublication(
    relativeResourcePlacement: Project.() -> Provider<File> = {
        project.provider {
            File("embed/${project.name}")
        }
    }
) {
    val publication = project.extraProperties.get(
        KotlinTargetResourcesPublication.EXTENSION_NAME
    ) as KotlinTargetResourcesPublication
    targets.matching {
        publication.canPublishResources(it)
    }.configureEach { target ->
        val fontsFilter = if (target is KotlinAndroidTarget) listOf("fonts/*") else emptyList()
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
            relativeResourcePlacement = project.relativeResourcePlacement(),
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
                relativeResourcePlacement = project.relativeResourcePlacement(),
            )
        }
    }
}