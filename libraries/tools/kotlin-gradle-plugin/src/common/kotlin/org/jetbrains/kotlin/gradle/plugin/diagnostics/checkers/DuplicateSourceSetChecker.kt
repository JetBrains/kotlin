/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector

/**
 * This checker is used to ensure that there are no duplicate source sets in MPP project.
 *
 * Examples of duplicated sourceSets: [jvmMain and jVmMain] or [macosMain and macOSMain]
 *
 */
// TODO(Dmitrii Krasnov): Remove this checker once IDEA-317606 is resolved
internal object DuplicateSourceSetChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val kotlin = project.multiplatformExtensionOrNull ?: return
        val sourceSetNames = kotlin.awaitSourceSets().map { it.name }
        val duplicateSourceSets = sourceSetNames
            .groupBy { it.toLowerCase() }
            .filter { (_, values) -> values.size > 1 }

        if (duplicateSourceSets.isNotEmpty()) {
            project.reportDiagnostic(KotlinToolingDiagnostics.DuplicateSourceSetsError(duplicateSourceSets))
        }
    }
}