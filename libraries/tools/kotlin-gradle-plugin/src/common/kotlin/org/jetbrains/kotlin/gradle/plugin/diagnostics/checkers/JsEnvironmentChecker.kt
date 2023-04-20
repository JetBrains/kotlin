/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmSubTargetContainerDsl

internal object JsEnvironmentChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val misconfiguredTarget = multiplatformExtension?.findMisconfiguredTargetIfAny() ?: return

        collector.reportOncePerGradleBuild(
            project,
            KotlinToolingDiagnostics.JsEnvironmentNotChosenExplicitly(
                availableEnvironments = listOfNotNull(
                    "browser()",
                    "nodejs()",
                    "d8".takeIf { misconfiguredTarget.platformType == KotlinPlatformType.wasm }
                )
            )
        )
    }

    private suspend fun KotlinMultiplatformExtension.findMisconfiguredTargetIfAny(): KotlinTarget? {
        return awaitTargets()
            .filter { it.platformType == KotlinPlatformType.js || it.platformType == KotlinPlatformType.wasm }
            .find {
                it.browserNotConfigured() && it.nodejsNotConfigured() && it.d8NotConfigured()
            }
    }

    private fun KotlinTarget.browserNotConfigured() = (this as? KotlinJsSubTargetContainerDsl)?.isBrowserConfigured == false
    private fun KotlinTarget.nodejsNotConfigured() = (this as? KotlinJsSubTargetContainerDsl)?.isNodejsConfigured == false
    private fun KotlinTarget.d8NotConfigured() = (this as? KotlinWasmSubTargetContainerDsl)?.isD8Configured == false
}
