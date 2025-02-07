/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_RESOURCES_RESOLUTION_STRATEGY
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector

internal object GradleDeprecatedPropertyChecker : KotlinGradleProjectChecker {
    private val warningDeprecatedProperties: List<String> = listOf(
        "kotlin.useK2",
        "kotlin.experimental.tryK2",
        "kotlin.incremental.classpath.snapshot.enabled",
        "kotlin.internal.single.build.metrics.file",
        "kotlin.build.report.dir",
        "kotlin.native.ignoreIncorrectDependencies",
        "kotlin.wasm.stability.nowarn",
        KotlinJsCompilerType.jsCompilerProperty,
        "${KotlinJsCompilerType.jsCompilerProperty}.nowarn",
        "kotlin.mpp.androidGradlePluginCompatibility.nowarn", // Since 2.1.0
    )

    private val errorDeprecatedProperties: List<String> = listOf(
        KOTLIN_MPP_RESOURCES_RESOLUTION_STRATEGY,
    )

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val propertiesBuildService = PropertiesBuildService.registerIfAbsent(project).get()

        propertiesBuildService.onlyUsed(project, warningDeprecatedProperties).forEach {
            collector.reportOncePerGradleBuild(
                project,
                KotlinToolingDiagnostics.DeprecatedWarningGradleProperties(it),
                it
            )
        }

        propertiesBuildService.onlyUsed(project, errorDeprecatedProperties).forEach {
            collector.reportOncePerGradleBuild(
                project,
                KotlinToolingDiagnostics.DeprecatedErrorGradleProperties(it),
                it
            )
        }
    }

    private fun PropertiesBuildService.onlyUsed(project: Project, properties: List<String>): List<String> = properties.filter {
        get(it, project) != null
    }
}
