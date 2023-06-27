/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.targets.native.internal.*
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerDependent
import org.jetbrains.kotlin.gradle.targets.native.internal.from
import org.jetbrains.kotlin.gradle.targets.native.internal.isAllowCommonizer

internal object DisabledCinteropCommonizationInHmppProjectChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        // `isAllowCommonizer` is not lifecycle-aware, but requires afterEvaluate-phase, so have to await manually
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        if (multiplatformExtension == null
            || !project.isAllowCommonizer()
            || project.cInteropCommonizationEnabled()
            || kotlinPropertiesProvider.ignoreDisabledCInteropCommonization
        ) return

        val sharedCompilationsWithInterops = multiplatformExtension.awaitTargets()
            .flatMap { it.compilations }
            .filterIsInstance<KotlinSharedNativeCompilation>()
            .mapNotNull { compilation ->
                val cinteropDependent = CInteropCommonizerDependent.from(compilation) ?: return@mapNotNull null
                compilation to cinteropDependent
            }
            .toMap()

        val affectedCompilations = sharedCompilationsWithInterops.keys
        val affectedCInterops = sharedCompilationsWithInterops.values.flatMap { it.interops }.toSet()

        /* CInterop commonizer would not affect the project: No compilation that would actually benefit */
        if (affectedCompilations.isEmpty()) return
        if (affectedCInterops.isEmpty()) return

        val affectedSourceSetsString = affectedCompilations.map { it.defaultSourceSet.name }.sorted().joinToString(", ", "[", "]")
        val affectedCinteropsString = affectedCInterops.map { it.toString() }.sorted().joinToString(", ", "[", "]")

        collector.reportOncePerGradleProject(
            project,
            KotlinToolingDiagnostics.DisabledCinteropsCommonizationInHmppProject(affectedSourceSetsString, affectedCinteropsString)
        )
    }
}
