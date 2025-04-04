/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_RESOURCES_RESOLUTION_STRATEGY
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector

internal object GradleDeprecatedPropertyChecker : KotlinGradleProjectChecker {
    private class DeprecatedProperty(
        val propertyName: String,
        val details: String? = null,
    )

    private val warningDeprecatedProperties: List<DeprecatedProperty> = listOf(
        DeprecatedProperty("kotlin.useK2"),
        DeprecatedProperty("kotlin.experimental.tryK2"),
        DeprecatedProperty("kotlin.incremental.classpath.snapshot.enabled"),
        DeprecatedProperty("kotlin.internal.single.build.metrics.file"),
        DeprecatedProperty("kotlin.build.report.dir"),
        DeprecatedProperty("kotlin.native.ignoreIncorrectDependencies"),
        DeprecatedProperty("kotlin.wasm.stability.nowarn"),
        DeprecatedProperty(KotlinJsCompilerType.jsCompilerProperty),
        DeprecatedProperty("${KotlinJsCompilerType.jsCompilerProperty}.nowarn"),
        DeprecatedProperty("kotlin.mpp.androidGradlePluginCompatibility.nowarn"), // Since 2.1.0
        DeprecatedProperty("kotlin.experimental.swift-export.enabled"),
        DeprecatedProperty(
            "kotlin.native.enableKlibsCrossCompilation",
            "Klibs cross compilation is enabled by default. " +
                    "You can disable it by setting the property `kotlin.native.disableKlibsCrossCompilation` to true.",
        ), // Since 2.2.20
        DeprecatedProperty(
            propertyName = "kotlin.incremental.useClasspathSnapshot",
            details = "History based incremental compilation approach for JVM platform is removed." +
                    " Kotlin Gradle plugin is now using a more efficient approach based on ABI snapshots."
        ),
    )

    private val errorDeprecatedProperties: List<DeprecatedProperty> = listOf(
        DeprecatedProperty(
            KOTLIN_MPP_RESOURCES_RESOLUTION_STRATEGY,
            "Resolution strategy for resources shouldn't be specified. See https://youtrack.jetbrains.com/issue/KT-66133 for details.",
        ),
        DeprecatedProperty(
            KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION,
            "See https://kotl.in/KT-75161 for details.",
        ),
        DeprecatedProperty(
            KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION,
            "See https://kotl.in/KT-75161 for details.",
        ),
    )

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val propertiesBuildService = PropertiesBuildService.registerIfAbsent(project).get()

        warningDeprecatedProperties.filter {
            propertiesBuildService.isPropertyUsed(project, it.propertyName)
        }.forEach {
            collector.reportOncePerGradleBuild(
                project,
                KotlinToolingDiagnostics.DeprecatedWarningGradleProperties(
                    it.propertyName,
                    it.details,
                ),
                key = it.propertyName,
            )
        }

        errorDeprecatedProperties.filter {
            propertiesBuildService.isPropertyUsed(project, it.propertyName)
        }.forEach {
            collector.reportOncePerGradleBuild(
                project,
                KotlinToolingDiagnostics.DeprecatedErrorGradleProperties(
                    it.propertyName,
                    it.details,
                ),
                key = it.propertyName,
            )
        }
    }

    private fun PropertiesBuildService.isPropertyUsed(project: Project, property: String): Boolean = get(property, project) != null
}
