/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.junit.Test

class PreciseCompilationOutputsBackupTest {
    @Test
    fun testEnablingPreciseOutputsBackupProducesWarning() {
        testPropertyUsage("kotlin.compiler.preciseCompilationResultsBackup", "true")
    }

    @Test
    fun testDisablingPreciseOutputsBackupProducesWarning() {
        testPropertyUsage("kotlin.compiler.preciseCompilationResultsBackup", "false")
    }

    @Test
    fun testEnablingInMemoryCachesProducesWarning() {
        testPropertyUsage("kotlin.compiler.keepIncrementalCompilationCachesInMemory", "true")
    }

    @Test
    fun testDisablingInMemoryCachesProducesWarning() {
        testPropertyUsage("kotlin.compiler.keepIncrementalCompilationCachesInMemory", "false")
    }

    private fun testPropertyUsage(propertyKey: String, propertyValue: String) {
        val project = buildProjectWithJvm(preApplyCode = {
            extraProperties.set(propertyKey, propertyValue)
        }).evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedWarningGradleProperties)
    }

    @Test
    fun testDefaultValuesDoNotProduceWarning() {
        buildProjectWithJvm {
            assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedWarningGradleProperties)
            assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedErrorGradleProperties)
        }
    }
}