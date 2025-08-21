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

internal object CinteropCrossCompilationChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        if (multiplatformExtension == null || !kotlinPropertiesProvider.enableKlibsCrossCompilation) return

        multiplatformExtension.targets
            .withType(KotlinNativeTarget::class.java)
            .matching { !HostManager().isEnabled(it.konanTarget) }
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