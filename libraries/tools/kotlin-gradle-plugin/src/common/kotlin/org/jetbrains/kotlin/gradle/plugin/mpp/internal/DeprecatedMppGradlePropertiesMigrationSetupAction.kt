/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_DEPENDENCY_PROPAGATION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector

internal val DeprecatedMppGradlePropertiesMigrationSetupAction = KotlinProjectSetupAction {
    checkAndReportDeprecatedMppProperties(project)
    handleHierarchicalStructureFlagsMigration(project)
}

/**
 * Declared properties have to be captured during plugin application phase before the HMPP migration util sets them.
 * Warnings have to be reported only for successfully evaluated projects without errors.
 */
private fun checkAndReportDeprecatedMppProperties(project: Project) {
    val projectProperties = project.kotlinPropertiesProvider

    val usedProperties = deprecatedMppProperties.mapNotNull { propertyName ->
        if (propertyName in propertiesSetByPlugin && projectProperties.mpp13XFlagsSetByPlugin)
            return@mapNotNull null

        propertyName.takeIf { projectProperties.property(propertyName).orNull != null }
    }

    if (usedProperties.isEmpty()) return

    project.kotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(
        project,
        KotlinToolingDiagnostics.PreHMPPFlagsError(usedProperties)
    )
}

internal val deprecatedMppProperties: List<String> = listOf(
    KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT,
    KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA,
    KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT,
    KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT,
    KOTLIN_NATIVE_DEPENDENCY_PROPAGATION,
)

private val propertiesSetByPlugin: Set<String> = setOf(
    KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA,
)
