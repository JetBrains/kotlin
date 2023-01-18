/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_DEPENDENCY_PROPAGATION
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.gradle.utils.hasSyncErrors
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheckWhenEvaluated
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.core.UnsafeApi

internal fun checkAndReportDeprecatedNativeTargets(project: Project) {
    project.runProjectConfigurationHealthCheckWhenEvaluated {
        val targets = project.extensions.getByType(KotlinMultiplatformExtension::class.java).targets
        val usedDeprecatedTargets = targets.filter { it is KotlinNativeTarget && it.konanTarget in KonanTarget.deprecatedTargets }
        if (usedDeprecatedTargets.isEmpty()) return@runProjectConfigurationHealthCheckWhenEvaluated
        SingleWarningPerBuild.show(
            project,
            "w: The following deprecated kotlin native targets were used in the project: ${usedDeprecatedTargets.joinToString { it.targetName }}"
        )
    }
}

/**
 * Declared properties have to be captured during plugin application phase before the HMPP migration util sets them.
 * Warnings have to be reported only for successfully evaluated projects without errors.
 */
internal fun checkAndReportDeprecatedMppProperties(project: Project) {
    val projectProperties = project.kotlinPropertiesProvider
    if (projectProperties.ignoreHmppDeprecationWarnings == true) return

    val warnings = deprecatedMppProperties.mapNotNull { propertyName ->
        @OptIn(UnsafeApi::class)
        projectProperties.property(propertyName)?.let { getMppDeprecationWarningMessageForProperty(propertyName) }
    }

    project.whenEvaluated {
        if (!project.hasSyncErrors()) {
            warnings.forEach { message ->
                SingleWarningPerBuild.show(project, message)
            }
        }
    }
}

internal val deprecatedMppProperties: List<String> = listOf(
    KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT,
    KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA,
    KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT,
    KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT,
    KOTLIN_NATIVE_DEPENDENCY_PROPAGATION,
)

internal fun getMppDeprecationWarningMessageForProperty(property: String): String =
    "w: The property '$property' is deprecated and is scheduled for removal in the stable Kotlin Multiplatform."
