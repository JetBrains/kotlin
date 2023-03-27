/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.utils.toMap

/**
 * Report scenario when there are two targets of the same platform without distinguishing attribute
 */
internal object TargetsWithAmbiguousConsumableConfigurationsChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        // Need all configurations to be created/set up
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val allTargets = multiplatformExtension?.targets ?: return

        val nonDistinguishableTargets = allTargets
            .mapNotNull { target ->
                val configuration = project.configurations.findByName(target.apiElementsConfigurationName) ?: return@mapNotNull null
                target.name to configuration
            }
            .groupBy { (_, consumableConfiguration) -> consumableConfiguration.attributes.toMap() }
            .values
            .filter { targetGroup -> targetGroup.size > 1 }
            .map { targetGroup -> targetGroup.map { (targetName, _) -> targetName } }

        if (nonDistinguishableTargets.isEmpty()) return

        val nonUniqueTargetsString = nonDistinguishableTargets.joinToString(separator = "\n") { targets ->
            val targetsListString = targets.joinToString { targetName -> "'$targetName'" }
            "  * $targetsListString"
        }

        collector.reportOncePerGradleProject(project, KotlinToolingDiagnostics.TargetsNeedDisambiguation(nonUniqueTargetsString))
    }
}
