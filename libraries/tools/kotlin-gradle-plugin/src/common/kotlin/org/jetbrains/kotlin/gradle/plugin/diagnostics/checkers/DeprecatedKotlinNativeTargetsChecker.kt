/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget

internal object DeprecatedKotlinNativeTargetsChecker : KotlinGradleProjectChecker {
    override fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val targets = multiplatformExtension?.targets ?: return
        val usedDeprecatedTargets = targets
            .filter { it is KotlinNativeTarget && it.konanTarget in KonanTarget.deprecatedTargets }
            .map { it.name }
        if (usedDeprecatedTargets.isEmpty()) return

        collector.report(project, KotlinToolingDiagnostics.DeprecatedKotlinNativeTargetsDiagnostic(usedDeprecatedTargets))
    }
}
