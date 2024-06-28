/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object UnusedSourceSetsChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val unusedSourceSets = multiplatformExtension?.awaitSourceSets().orEmpty()
            // Ignoring Android source sets
            .filter { it.androidSourceSetInfoOrNull == null }
            .filter { it.internal.awaitPlatformCompilations().isEmpty() }

        if (unusedSourceSets.isNotEmpty()) {
            collector.report(project, KotlinToolingDiagnostics.UnusedSourceSetsWarning(unusedSourceSets.toSet().map { it.name }))
        }
    }
}
