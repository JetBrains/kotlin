/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.TestInfrastructureException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins what the WASI `test.mjs` launcher calls, since the two candidates are not interchangeable and the export names
 * cannot tell them apart: `startTest()` is both the grouped result-collecting driver and — via the `wasiBoxTestRun.kt`
 * additional file — the `box()` helper of every box test.
 */
class WasiLauncherScriptTest {
    @Test
    fun `given a batch with the generated driver then the launcher calls it`() {
        val script = startUnitTestsWasiScript(callGroupedTestsDriver = true)

        assertTrue("jsModule.startTest();" in script, script)
        // The driver reports each result through stdout, so the unit-test runner must not be driven on top of it.
        assertFalse("startUnitTests" in script, script)
    }

    @Test
    fun `given a batch without the driver then the launcher calls the unit-test runner`() {
        // The case an export-name probe got wrong: an isolated `// RUN_UNIT_TESTS` test that also has a `box()` exports
        // `startTest` from `wasiBoxTestRun.kt`, and calling that would run `box()` and skip the unit tests entirely.
        val script = startUnitTestsWasiScript(callGroupedTestsDriver = false)

        assertTrue("jsModule.startUnitTests();" in script, script)
        assertFalse("startTest" in script, script)
    }

    @Test
    fun `given a grouped binary exporting only the driver then the export-surface check passes`(@TempDir dir: File) {
        // The export list a grouped link actually produces: the driver's `startTest`, no helper exports.
        dir.resolve("index.mjs").writeText("export const {\n    startTest,\n    startUnitTests\n} = exports\n")

        assertDoesNotThrow { assertDriverOwnsStartTestExport(dir) }

        // No glue at all is not this check's business (e.g. a mode that produced no JS glue).
        assertDoesNotThrow { assertDriverOwnsStartTestExport(dir.resolve("no-such-subdir")) }
    }

    @Test
    fun `given a helper export leaking into a grouped binary then the export-surface check fails`(@TempDir dir: File) {
        // What a broken link would produce: `wasiBoxTestRun.kt`'s exports reached the linked module, so the bare
        // `startTest` the standalone VMs invoke is no longer unambiguously the driver.
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
