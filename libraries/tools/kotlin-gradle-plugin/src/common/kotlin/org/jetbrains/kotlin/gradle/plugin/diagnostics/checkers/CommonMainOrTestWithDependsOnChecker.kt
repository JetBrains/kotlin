/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object CommonMainOrTestWithDependsOnChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        if (multiplatformExtension == null) return
        project.configurationResult.await()

        fun KotlinSourceSet.checkAndReport(suffix: String) {
            if (internal.dependsOn.isNotEmpty()) {
                collector.report(
                    project,
                    KotlinToolingDiagnostics.CommonMainOrTestWithDependsOnDiagnostic(suffix),
                )
            }
        }

        multiplatformExtension.sourceSets.forEach {
            when (it.name) {
                KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME -> it.checkAndReport("Main")
                KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME -> it.checkAndReport("Test")
            }
        }
    }
}
