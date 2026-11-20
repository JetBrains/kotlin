/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WasiBoxRunnerTest {
    @Test
    fun `given a unit-test run whose output shows no test starting then the run is rejected`() {
        val error = checkUnitTestsReported(output = "box output\n", executionName = "Wasmtime (dev)")

        val message = error?.message.orEmpty()
        assertTrue("reported no test in Wasmtime (dev)" in message, message)
        assertTrue(UNIT_TEST_STARTED_MARKER in message, message)
        assertTrue("box output" in message, message)
    }

    @Test
    fun `given a unit-test run whose output shows a test starting then the run is accepted`() {
        val output = "##teamcity[testSuiteStarted name='' flowId='f']\n" +
                "##teamcity[testStarted name='runTest' flowId='f']\n" +
                "##teamcity[testFinished name='runTest' flowId='f']\n"

        assertEquals(null, checkUnitTestsReported(output, executionName = "WasmEdge (dev)"))
    }
}
