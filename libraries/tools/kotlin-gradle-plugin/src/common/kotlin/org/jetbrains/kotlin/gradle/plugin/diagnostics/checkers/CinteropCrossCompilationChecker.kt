/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

internal object CinteropCrossCompilationChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        AfterFinaliseDsl.await()

        val extension = multiplatformExtension ?: return

        extension.targets
            .withType(KotlinNativeTarget::class.java)
            .matching { !HostManager().isEnabled(it.konanTarget) }
            .configureEach { target ->
                target.compilations.configureEach { compilation ->
                    if (compilation.cinterops.isNotEmpty()) {
                        collector.report(
                            project,
                            KotlinToolingDiagnostics.CrossCompilationWithCinterops(
                                if (HostManager.hostIsMac) WARNING else ERROR,
                                target.targetName,
                                compilation.cinterops.map { it.name },
                                HostManager.hostName
                            )
                        )
                    }
                }
            }
    }
}