/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinTargetAlreadyDeclared
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset

internal object KotlinTargetAlreadyDeclaredChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val targets = multiplatformExtension?.awaitTargets() ?: return
        val duplicatedTargets = targets
            .filter { it !is KotlinMetadataTarget }
            .groupBy {
                @Suppress("DEPRECATION")
                it.preset?.name
            }
            .filterValues { it.size > 1 }

        for (targetsGroup in duplicatedTargets.values) {
            val targetDslFunctionName = targetsGroup.first()
                .targetDslFunctionName
                // skip targets without known dsl function such as external targets
                ?: continue

            collector.report(project, KotlinTargetAlreadyDeclared(targetDslFunctionName))
        }
    }

    /**
     * DSL names are taken from [org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions]
     */
    @Suppress("DEPRECATION")
    private val KotlinTarget.targetDslFunctionName
        get() = when (preset) {
            is KotlinJsIrTargetPreset -> "js"
            is org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsTargetPreset -> "js"
            is KotlinAndroidTargetPreset -> "androidTarget"
            else -> preset?.name
        }
}