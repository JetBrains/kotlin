/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.targets.android.findAndroidTarget
import org.jetbrains.kotlin.gradle.utils.androidPluginIds
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheck

private class KotlinMultiplatformProjectConfigurationException(message: String) : Exception(message)

internal fun Project.runMissingKotlinTargetsProjectConfigurationHealthCheck() = project.runProjectConfigurationHealthCheck {
    val isNoTargetsInitialized = (project.kotlinExtension as KotlinMultiplatformExtension)
        .targets
        .none { it !is KotlinMetadataTarget }

    if (isNoTargetsInitialized) {
        throw KotlinMultiplatformProjectConfigurationException(
            """
                Please initialize at least one Kotlin target in '${project.name} (${project.path})'.
                Read more https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets
                """.trimIndent()
        )
    }
}

internal fun Project.runMissingAndroidTargetProjectConfigurationHealthCheck(
    warningLogger: (warningMessage: String) -> Unit = project.logger::warn
) = project.runProjectConfigurationHealthCheck check@{
    if (project.kotlinPropertiesProvider.ignoreAbsentAndroidMultiplatformTarget) {
        return@check
    }

    val androidPluginId = androidPluginIds
        .firstOrNull { androidPluginId -> plugins.findPlugin(androidPluginId) != null } ?: return@check

    if (findAndroidTarget() != null) return@check

    warningLogger(
        """
            Missing 'android()' Kotlin target in multiplatform project ${project.name} (${project.path})'.
            The Android Gradle plugin was applied without creating a corresponding 'android()' Kotlin Target:
            
            ```
            plugins {
                id("$androidPluginId")
                kotlin("multiplatform")
            }
            
            kotlin {
                android() // <-- please register this Android target
            }
            ```
          
        """.trimIndent()
    )
}
