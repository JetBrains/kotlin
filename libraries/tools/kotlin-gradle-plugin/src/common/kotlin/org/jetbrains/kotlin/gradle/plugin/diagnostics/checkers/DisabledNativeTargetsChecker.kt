/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForKlibCompilation

internal object DisabledNativeTargetsChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        if (multiplatformExtension == null || kotlinPropertiesProvider.ignoreDisabledNativeTargets == true) return

        val disabledTargets = multiplatformExtension.awaitTargets()
            .filterIsInstance<KotlinNativeTarget>()
            .filter { !it.enabledOnCurrentHostForKlibCompilation }
            .map { it.name }

        if (disabledTargets.isNotEmpty()) {
            collector.reportOncePerGradleProject(project, KotlinToolingDiagnostics.DisabledKotlinNativeTargets(disabledTargets))
        }
    }
}
