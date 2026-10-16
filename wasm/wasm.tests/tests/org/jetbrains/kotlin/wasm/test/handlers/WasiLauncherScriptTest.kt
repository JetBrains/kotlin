/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WasiLauncherScriptTest {
    @Test
    fun `given a batch with the generated driver then the launcher calls it`() {
        val script = startUnitTestsWasiScript(callGroupedTestsDriver = true)

        assertTrue("jsModule.startTest();" in script, script)
        assertFalse("startUnitTests" in script, script)
    }

    @Test
    fun `given a batch without the driver then the launcher calls the unit-test runner`() {
        val script = startUnitTestsWasiScript(callGroupedTestsDriver = false)

        assertTrue("jsModule.startUnitTests();" in script, script)
        assertFalse("startTest" in script, script)
    }

    @Test
    fun `given a driverless unit-test run then the standalone VMs invoke the unit-test runner export`() {
        assertEquals("startUnitTests", wasiStandaloneEntryExport(hasGroupedTestsDriver = false, runUnitTests = true))
    }

    @Test
    fun `given a driver-linked batch then the standalone VMs invoke the driver even when unit tests are requested`() {
        assertEquals("startTest", wasiStandaloneEntryExport(hasGroupedTestsDriver = true, runUnitTests = true))
        assertEquals("startTest", wasiStandaloneEntryExport(hasGroupedTestsDriver = true, runUnitTests = false))
    }

    @Test
    fun `given a driverless box run then the standalone VMs invoke the box helper`() {
        assertEquals("startTest", wasiStandaloneEntryExport(hasGroupedTestsDriver = false, runUnitTests = false))
    }

    @Test
    fun `given the Node launcher for a driverless unit-test run then it calls the same export the standalone VMs do`() {
        val export = wasiStandaloneEntryExport(hasGroupedTestsDriver = false, runUnitTests = true)

        assertTrue("jsModule.$export();" in startUnitTestsWasiScript(callGroupedTestsDriver = false))
    }

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
