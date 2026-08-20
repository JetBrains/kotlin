/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.TestInfrastructureException
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins what the WASI `test.mjs` launcher calls: `startTest()` is both the grouped driver and — via
 * `wasiBoxTestRun.kt` — the `box()` helper of every box test, so the export names cannot tell the two apart.
 */
class WasiLauncherScriptTest {
    @Test
    fun `given a batch with the generated driver then the launcher calls it`() {
        val script = startUnitTestsWasiScript(callGroupedTestsDriver = true)

        assertTrue("jsModule.startTest();" in script, script)
        // The driver reports through stdout, so the unit-test runner must not be driven on top of it.
        assertFalse("startUnitTests" in script, script)
    }

    @Test
    fun `given a batch without the driver then the launcher calls the unit-test runner`() {
        // The case an export-name probe got wrong: calling `startTest` here would run `box()` instead of the unit tests.
        val script = startUnitTestsWasiScript(callGroupedTestsDriver = false)

        assertTrue("jsModule.startUnitTests();" in script, script)
        assertFalse("startTest" in script, script)
    }

    @Test
    fun `given a driverless unit-test run on a standalone VM then the run is rejected`() {
        // Those VMs invoke the bare `startTest` export, which without a driver is `wasiBoxTestRun.kt`'s `box()`
        // helper: the unit tests would not run at all and the batch would still be green.
        val error = assertThrows(TestInfrastructureException::class.java) {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = false,
                runUnitTests = true,
                vmsToCheck = listOf(WasmVM.NodeJs, WasmVM.WasmEdge, WasmVM.Wasmtime),
            )
        }

        val message = error.message.orEmpty()
        assertTrue(WasmVM.WasmEdge.vmName in message, message)
        assertTrue(WasmVM.Wasmtime.vmName in message, message)
        assertFalse(WasmVM.NodeJs.vmName in message, message)
    }

    @Test
    fun `given a driver or a JS-entry-point VM then the unit-test run is accepted`() {
        // With the driver linked in, `startTest` is the driver itself; Node.js goes through `test.mjs` either way.
        assertDoesNotThrow {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = true,
                runUnitTests = true,
                vmsToCheck = listOf(WasmVM.WasmEdge, WasmVM.Wasmtime),
            )
        }
        assertDoesNotThrow {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = false,
                runUnitTests = true,
                vmsToCheck = listOf(WasmVM.NodeJs),
            )
        }
        assertDoesNotThrow {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = false,
                runUnitTests = false,
                vmsToCheck = listOf(WasmVM.WasmEdge),
            )
        }
    }

    @Test
    fun `given an explicit unit-test-only invocation on a standalone VM then it is rejected`() {
        val error = assertThrows(TestInfrastructureException::class.java) {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = false,
                runUnitTests = true,
                vmsToCheck = listOf(WasmVM.Wasmtime),
            )
        }

        assertTrue("unit-test runner" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given a grouped binary exporting only the driver then the export-surface check passes`(@TempDir dir: File) {
        dir.resolve("index.mjs").writeText("export const {\n    startTest,\n    startUnitTests\n} = exports\n")

        assertDoesNotThrow { assertDriverOwnsStartTestExport(dir) }

        // No glue at all is not this check's business (e.g. a mode that produced none).
        assertDoesNotThrow { assertDriverOwnsStartTestExport(dir.resolve("no-such-subdir")) }
    }

    @Test
    fun `given a helper export leaking into a grouped binary then the export-surface check fails`(@TempDir dir: File) {
        dir.resolve("index.mjs").writeText("export const {\n    runBoxTest,\n    startTest\n} = exports\n")

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("runBoxTest" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given a grouped binary without a startTest export then the export-surface check fails`(@TempDir dir: File) {
        dir.resolve("index.mjs").writeText("export const {\n    startUnitTests\n} = exports\n")

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("does not export `startTest`" in error.message.orEmpty(), error.message.orEmpty())
    }
}
