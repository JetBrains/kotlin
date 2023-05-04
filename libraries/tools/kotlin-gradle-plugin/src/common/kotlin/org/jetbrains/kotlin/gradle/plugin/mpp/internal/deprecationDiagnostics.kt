/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_GRANULAR_SOURCE_SETS_METADATA
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_SUPPORT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_DEPENDENCY_PROPAGATION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheckWhenEvaluated
import org.jetbrains.kotlin.gradle.utils.toMap
import org.jetbrains.kotlin.tooling.core.UnsafeApi

internal fun runDeprecationDiagnostics(project: Project) {
    checkAndReportDeprecatedMppProperties(project)
    handleHierarchicalStructureFlagsMigration(project)
    project.runProjectConfigurationHealthCheckWhenEvaluated {
        reportTargetsWithNonUniqueConsumableConfigurations(project)
        checkAndReportPreHmppDependenciesUsage(project)
    }
}

/**
 * Report scenario when there are two targets of the same platform without distinguishing attribute
 */
private fun reportTargetsWithNonUniqueConsumableConfigurations(project: Project) {
    // Wrap diagnostic check again to afterEvaluate to make sure that it gets executed the last
    // Since Multiplatform plugin updates consumable configurations in afterEvaluate blocks
    project.afterEvaluate {
        val allTargets = project.multiplatformExtension.targets

        val nonDistinguishableTargets = allTargets
            .mapNotNull { target ->
                val configuration = project.configurations.findByName(target.apiElementsConfigurationName) ?: return@mapNotNull null
                target.name to configuration
            }
            .groupBy { (_, consumableConfiguration) -> consumableConfiguration.attributes.toMap() }
            .values
            .filter { targetGroup -> targetGroup.size > 1 }
            .map { targetGroup -> targetGroup.map { (targetName, _) -> targetName } }

        if (nonDistinguishableTargets.isEmpty()) return@afterEvaluate

        val nonUniqueTargetsString = nonDistinguishableTargets.joinToString(separator = "\n") { targets ->
            val targetsListString = targets.joinToString { targetName -> "'$targetName'" }
            "  * $targetsListString"
        }

        SingleWarningPerBuild.show(
            project, "w: The following targets are not distinguishable:\n$nonUniqueTargetsString" +
                    "\nUse distinguish attribute. " +
                    "See https://kotlinlang.org/docs/multiplatform-set-up-targets.html#distinguish-several-targets-for-one-platform for more details."
        )
    }
}

/**
 * Declared properties have to be captured during plugin application phase before the HMPP migration util sets them.
 * Warnings have to be reported only for successfully evaluated projects without errors.
 */
private fun checkAndReportDeprecatedMppProperties(project: Project) {
    val projectProperties = project.kotlinPropertiesProvider
    if (projectProperties.ignoreHmppDeprecationWarnings == true) return

    val usedProperties = deprecatedMppProperties.mapNotNull { propertyName ->
        if (propertyName in propertiesSetByPlugin && projectProperties.mpp13XFlagsSetByPlugin)
            return@mapNotNull null

        @OptIn(UnsafeApi::class)
        propertyName.takeIf { projectProperties.property(propertyName) != null }
    }

    if (usedProperties.isEmpty()) return

    project.kotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(
        project,
        KotlinToolingDiagnostics.HierarchicalMultiplatformFlagsWarning(usedProperties)
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
    KOTLIN_NATIVE_DEPENDENCY_PROPAGATION,
)
