/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary

internal object NativeBinaryConfigurationChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        if (multiplatformExtension == null) return
        KotlinPluginLifecycle.Stage.AfterFinaliseDsl.await()

        multiplatformExtension.targets
            .withType(KotlinNativeTarget::class.java)
            .configureEach { target ->
                target.binaries.configureEach { binary ->
                    if (hasIncompatibleConfiguration(binary)) {
                        collector.report(
                            project,
                            KotlinToolingDiagnostics.IncompatibleBinaryConfiguration(
                                binary.name,
                                binary.debuggable,
                                binary.optimized
                            )
                        )
                    }
                }
            }
    }

    private fun hasIncompatibleConfiguration(binary: NativeBinary): Boolean {
        return (binary.debuggable && binary.optimized) || (!binary.debuggable && !binary.optimized)
    }
}