/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetSupportException

internal object CinteropCrossCompilationChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        if (multiplatformExtension == null || !kotlinPropertiesProvider.enableKlibsCrossCompilation) return

        multiplatformExtension.targets
            .withType(KotlinNativeTarget::class.java)
            .matching { target ->
                try {
                    // This is the normal path:
                    // The host is supported, and we are checking if it can
                    // compile the specific 'target.konanTarget'.
                    // We match if it's NOT enabled (i.e., it's a cross-compilation target).
                    !HostManager().isEnabled(target.konanTarget)
                } catch (_: TargetSupportException) {
                    // This means the HostManager().isEnabled failed because the *host itself* is unsupported.
                    false
                }
            }
            .configureEach { target ->
                var reportDiagnosticOncePerTarget = true
                target.compilations.configureEach { compilation ->
                    compilation.cinterops.configureEach {
                        if (reportDiagnosticOncePerTarget) {
                            reportDiagnosticOncePerTarget = false

                            val allCinterops = target.compilations
                                .flatMap { it.cinterops }
                                .map { it.name }
                                .distinct()

                            collector.report(
                                project,
                                KotlinToolingDiagnostics.CrossCompilationWithCinterops(
                                    project.name,
                                    target.targetName,
                                    allCinterops,
                                    HostManager.hostName
                                )
                            )
                        }
                    }
                }
            }
    }
}