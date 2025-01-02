/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Rule
import org.junit.Test

class PreciseCompilationOutputsBackupTest {
    @get:Rule val muteableTestRule = MuteableTestRule()

    @Test
    fun testDisablingPreciseOutputsBackupProducesWarning() {
        buildProjectWithJvm(preApplyCode = {
            extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_COMPILER_USE_PRECISE_COMPILATION_RESULTS_BACKUP, "false")
        }) {
            triggerPropertiesRead()
            assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedLegacyCompilationOutputsBackup)
        }
    }

    @Test
    fun testDisablingInMemoryCachesProducesWarning() {
        buildProjectWithJvm(preApplyCode = {
            extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_COMPILER_KEEP_INCREMENTAL_COMPILATION_CACHES_IN_MEMORY, "false")
        }) {
            triggerPropertiesRead()
            assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedLegacyCompilationOutputsBackup)
        }
    }

    @Test
    fun testDefaultValuesDoNotProduceWarning() {
        buildProjectWithJvm {
            triggerPropertiesRead()
            assertNoDiagnostics(KotlinToolingDiagnostics.DeprecatedLegacyCompilationOutputsBackup)
        }
    }

    private fun Project.triggerPropertiesRead() {
        tasks.withType(AbstractKotlinCompile::class.java).all {
            it.preciseCompilationResultsBackup.orNull
            it.keepIncrementalCompilationCachesInMemory.orNull
        }
    }
}