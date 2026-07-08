/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.nativeBinaryOptions.GC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeSecondStageCompilationConfigTest {
    @Test
    fun disabledEscapeAnalysisPropagateExiledToHeapObjectsWarnsForCmsAndGms() {
        for (gc in listOf(GC.CONCURRENT_MARK_AND_SWEEP, GC.GENERATIONAL_MARK_AND_SWEEP)) {
            val configuration = CompilerConfiguration.create()

            configuration.reportDisabledEscapeAnalysisPropagateExiledToHeapObjectsIfNeeded(value = false, gc)

            assertEquals(1, configuration.diagnosticsCollector.diagnostics.size, "gc=$gc")
            val diagnostic = configuration.diagnosticsCollector.diagnostics.single()
            assertEquals(CliDiagnostics.KONAN_ARGUMENT_STRONG_WARNING, diagnostic.factory, "gc=$gc")
            assertEquals(CMS_GMS_ESCAPE_ANALYSIS_PROPAGATE_EXILED_TO_HEAP_OBJECTS_WARNING, diagnostic.renderMessage(), "gc=$gc")
        }
    }

    @Test
    fun enabledEscapeAnalysisPropagateExiledToHeapObjectsDoesNotWarnForCmsAndGms() {
        for (gc in listOf(GC.CONCURRENT_MARK_AND_SWEEP, GC.GENERATIONAL_MARK_AND_SWEEP)) {
            val configuration = CompilerConfiguration.create()

            configuration.reportDisabledEscapeAnalysisPropagateExiledToHeapObjectsIfNeeded(value = true, gc)

            assertTrue(configuration.diagnosticsCollector.diagnostics.isEmpty(), "gc=$gc")
        }
    }

    @Test
    fun disabledEscapeAnalysisPropagateExiledToHeapObjectsDoesNotWarnForOtherCollectors() {
        for (gc in GC.entries - setOf(GC.CONCURRENT_MARK_AND_SWEEP, GC.GENERATIONAL_MARK_AND_SWEEP)) {
            val configuration = CompilerConfiguration.create()

            configuration.reportDisabledEscapeAnalysisPropagateExiledToHeapObjectsIfNeeded(value = false, gc)

            assertTrue(configuration.diagnosticsCollector.diagnostics.isEmpty(), "gc=$gc")
        }
    }
}
