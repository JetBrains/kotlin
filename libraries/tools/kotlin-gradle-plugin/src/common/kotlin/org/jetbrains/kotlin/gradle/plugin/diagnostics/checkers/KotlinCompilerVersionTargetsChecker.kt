/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * Kotlin/Native targets don't compile through the Build Tools API, so they ignore `kotlin.compilerVersion`.
 * Reports projects where that would leave targets compiled with different Kotlin compiler versions.
 */
internal object KotlinCompilerVersionTargetsChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val extension = multiplatformExtension ?: return
        // Wait until the build script is evaluated, so the compiler version is read without finalizing the property early
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()
        val nativeTargets = extension.awaitTargets().filterIsInstance<KotlinNativeTarget>().map { it.name }
        if (nativeTargets.isEmpty()) return

        val compilerVersion = extension.compilerVersion.orNull ?: return
        val pluginVersion = project.getKotlinPluginVersion()
        if (compilerVersion == pluginVersion) return

        collector.report(
            diagnosticsContext,
            KotlinToolingDiagnostics.KotlinCompilerVersionNotAppliedToNativeTargets(compilerVersion, pluginVersion, nativeTargets),
        )
    }
}
