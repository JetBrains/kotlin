/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset

internal object KotlinTargetAlreadyDeclaredChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val targets = multiplatformExtension?.awaitTargets() ?: return
        val duplicatedTargets = targets
            .filter { it !is KotlinMetadataTarget }
            .groupBy {
                it.internal._preset?.name
            }
            .filterValues { it.size > 1 }

        for (targetsGroup in duplicatedTargets.values) {
            val targetDslFunctionName = targetsGroup.first()
                .targetDslFunctionName
                // skip targets without known dsl function such as external targets
                ?: continue

            when (targetsGroup.first().internal._preset) {
                // For JS targets fire WARNING for now
                // FIXME: https://youtrack.jetbrains.com/issue/KT-59316/Deprecate-multiple-same-targets#focus=Comments-27-9992405.0-0
                is KotlinJsIrTargetPreset -> collector.report(
                    project,
                    KotlinToolingDiagnostics.KotlinTargetAlreadyDeclaredWarning(
                        targetDslFunctionName
                    )
                )
                else -> collector.report(
                    project,
                    KotlinToolingDiagnostics.KotlinTargetAlreadyDeclaredError(
                        targetDslFunctionName
                    )
                )
            }
        }
    }

    /**
     * DSL names are taken from [org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions]
     */
    @Suppress("DEPRECATION_ERROR")
    private val KotlinTarget.targetDslFunctionName
        get() = when (internal._preset) {
            is KotlinJsIrTargetPreset -> "js"
            is org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsTargetPreset -> "js"
            is KotlinAndroidTargetPreset -> "androidTarget"
            else -> internal._preset?.name
        }
}