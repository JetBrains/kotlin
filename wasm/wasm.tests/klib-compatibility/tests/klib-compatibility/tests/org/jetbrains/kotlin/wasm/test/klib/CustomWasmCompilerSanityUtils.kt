/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerTest

/**
 * Drives both compilation stages synchronously for a single test, mirroring what
 * `CompilerTestGroupingTestEngine` does for a single-sized batch.
 *
 * Generated box/boxInline tests are executed by the grouping test engine via
 * [AbstractTwoStageKotlinCompilerTest.initTestRunnerAndCreateModuleStructure]; this helper is used by the sanity tests
 * that need to assert synchronously on the outcome of a single test.
 */
internal fun AbstractTwoStageKotlinCompilerTest.runTest(@TestDataFile filePath: String) {
    initTestRunnerAndCreateModuleStructure(filePath)
    try {
        nonGroupingRunner.runTestPreprocessing()
        nonGroupingRunner.runSteps()

        // Report first-stage failures first (and throw on a real, non-suppressed failure). If the first stage
        // failed or was muted/ignored, the grouping (second) stage must be skipped, exactly like the grouping
        // test engine excludes such tests from the batch. Otherwise both stages would contribute failures and
        // they'd be aggregated into a `MultipleFailuresError` instead of the single expected exception.
        val hadIgnoredFailuresOnFirstStage = nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = false)
        if (hadIgnoredFailuresOnFirstStage) return

        val nonGroupingStageOutput = NonGroupingStageOutput(
            testServices = nonGroupingRunner.testServices,
            catchingExecutor = { wrapper, block ->
                nonGroupingRunner.failuresInterceptor.withAssertionCatching(wrapper, block)
            },
        )
        groupingStageRunner.run(listOf(nonGroupingStageOutput))

        // Exceptions from grouped facades were reported to the grouping runner's failures interceptor,
        // but failure suppressors must be run from the non-grouping runner, as they need access to the
        // real module structure of the specific test to extract directives from there.
        nonGroupingRunner.failuresInterceptor += groupingStageRunner.failuresInterceptor
        nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = true)
    } finally {
        nonGroupingRunner.finalizeAndDispose()
    }
}
