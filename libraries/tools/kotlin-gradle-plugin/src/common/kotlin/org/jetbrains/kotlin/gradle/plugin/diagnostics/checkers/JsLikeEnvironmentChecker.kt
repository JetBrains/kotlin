/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmSubTargetContainerDsl

internal abstract class JsLikeEnvironmentChecker(
    private val diagnostic: KotlinToolingDiagnostics.JsLikeEnvironmentNotChosenExplicitly,
    private val platformTypePredicate: (KotlinTarget) -> Boolean,
    private val environments: List<String>,
    private val environmentPredicates: List<(KotlinTarget) -> Boolean>,
) : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        multiplatformExtension?.findMisconfiguredTargetIfAny() ?: return

        collector.reportOncePerGradleBuild(
            project,
            diagnostic(
                availableEnvironments = environments
            )
        )
    }

    private suspend fun KotlinMultiplatformExtension.findMisconfiguredTargetIfAny(): KotlinTarget? {
        return awaitTargets()
            .filter(platformTypePredicate)
            .find { target ->
                environmentPredicates.all { it(target) }
            }
    }
}

internal fun KotlinTarget.browserNotConfigured() = (this as? KotlinJsSubTargetContainerDsl)?.isBrowserConfigured == false
internal fun KotlinTarget.nodejsNotConfigured() = (this as? KotlinJsSubTargetContainerDsl)?.isNodejsConfigured == false
internal fun KotlinTarget.d8NotConfigured() = (this as? KotlinWasmSubTargetContainerDsl)?.isD8Configured == false