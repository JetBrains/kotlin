/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector

internal object GradleDeprecatedPropertyChecker : KotlinGradleProjectChecker {
    private val deprecatedProperties: List<String> = listOf(
        "kotlin.useK2",
        "kotlin.experimental.tryK2",
        "kotlin.internal.single.build.metrics.file",
        "kotlin.build.report.dir",
        "kotlin.native.ignoreIncorrectDependencies",
    )

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val propertiesBuildService = PropertiesBuildService.registerIfAbsent(project).get()

        val usedDeprecatedProperties = deprecatedProperties.filter { propertiesBuildService.get(it, project) != null }

        usedDeprecatedProperties.forEach {
            collector.reportOncePerGradleBuild(
                project,
                KotlinToolingDiagnostics.DeprecatedGradleProperties(it),
                it
            )
        }
    }
}
