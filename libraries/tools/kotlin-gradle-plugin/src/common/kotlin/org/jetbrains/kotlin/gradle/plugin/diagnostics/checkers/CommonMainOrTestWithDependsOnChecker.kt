/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object CommonMainOrTestWithDependsOnChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        fun KotlinSourceSet.registerReporting(suffix: String) {
            internal.dependsOn.forAll {
                collector.reportOncePerGradleProject(
                    project,
                    KotlinToolingDiagnostics.CommonMainOrTestWithDependsOnDiagnostic(suffix),
                    key = suffix
                )
            }
        }
        multiplatformExtension?.sourceSets?.all {
            when (it.name) {
                KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME -> it.registerReporting("Main")
                KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME -> it.registerReporting("Test")
            }
        }
    }
}
