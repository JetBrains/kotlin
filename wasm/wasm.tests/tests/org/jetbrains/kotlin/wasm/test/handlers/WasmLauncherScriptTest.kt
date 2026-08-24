/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WasmLauncherScriptTest {
    @Test
    fun `given a batch with the generated driver then the launcher calls it`() {
        val invocation = generateWasmJsUnitTestRunnerInvocation(callGroupedTestsDriver = true)

        assertTrue("jsModule.runGroupedTests();" in invocation, invocation)
        assertFalse("startUnitTests" in invocation, invocation)
    }

    @Test
    fun `given a batch without the generated driver then the launcher calls unit tests even if the export exists`() {
        // A user-defined @JsExport with this name must not change the dispatch decision.
        val invocation = generateWasmJsUnitTestRunnerInvocation(callGroupedTestsDriver = false)

        assertTrue("jsModule.startUnitTests();" in invocation, invocation)
        assertFalse("runGroupedTests" in invocation, invocation)
        assertTrue("hasTestFailures" in invocation, invocation)
    }
}
