/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.wasm.test.blackbox.WasmJsGroupedTestsExportedEntryPointGenerator
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `given the generated wasm-js entry point then the launcher calls exactly the name it exports`() {
        val entryPoint = WasmJsGroupedTestsExportedEntryPointGenerator.generateExportedEntryPointSource("__runAll")

        // Renaming the export breaks every grouped wasm-js batch, and only the two sides together prove the name.
        assertTrue("@JsExport" in entryPoint, entryPoint)
        assertTrue("__runAll()" in entryPoint, entryPoint)

        val exportedName = EXPORTED_FUNCTION_NAME.find(entryPoint)?.groupValues?.get(1)
        assertEquals("runGroupedTests", exportedName, entryPoint)
        assertTrue(
            "jsModule.$exportedName();" in generateWasmJsUnitTestRunnerInvocation(callGroupedTestsDriver = true),
            entryPoint,
        )
    }

    @Test
    fun `given a batch without the generated driver then the launcher calls unit tests even if the export exists`() {
        // A user-defined @JsExport with this name must not change the dispatch decision.
        val invocation = generateWasmJsUnitTestRunnerInvocation(callGroupedTestsDriver = false)

        assertTrue("jsModule.startUnitTests();" in invocation, invocation)
        assertFalse("runGroupedTests" in invocation, invocation)
        assertTrue("hasTestFailures" in invocation, invocation)
    }

    private companion object {
        /** The name the VM invokes: whatever the generator declares is what the launcher script has to call. */
        val EXPORTED_FUNCTION_NAME = Regex("""fun\s+(\w+)\(""")
    }
}
