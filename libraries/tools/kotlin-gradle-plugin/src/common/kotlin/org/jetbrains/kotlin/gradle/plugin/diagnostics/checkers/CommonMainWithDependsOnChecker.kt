/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object CommonMainWithDependsOnChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        multiplatformExtension?.sourceSets?.all {
            if (it.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) {
                it.internal.dependsOn.forAll {
                    collector.reportOncePerGradleProject(project, KotlinToolingDiagnostics.CommonMainWithDependsOnDiagnostic())
                }
            }
        }
    }
}
