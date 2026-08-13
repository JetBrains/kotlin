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
    fun `given a grouped binary exporting only the driver then the export-surface check passes`(@TempDir dir: File) {
        dir.resolve("index.mjs").writeText("export const {\n    startTest,\n    startUnitTests\n} = exports\n")

        assertDoesNotThrow { assertDriverOwnsStartTestExport(dir) }

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
