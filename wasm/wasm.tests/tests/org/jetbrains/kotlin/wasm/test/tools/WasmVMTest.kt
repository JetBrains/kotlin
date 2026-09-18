/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.tools

import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WasmVMTest {
    @Test
    fun `given more output than the capture limit then only bounded output with its digest is retained`() {
        val capture = BoundedOutputCapture()
        val chunk = "x".repeat(8 * 1024).toCharArray()
        repeat((4 * 1024 * 1024) / chunk.size + 1) {
            capture.append(chunk, chunk.size)
        }

        val output = capture.toString()

        assertTrue(output.length <= 4 * 1024 * 1024 + 256, output.length.toString())
        assertTrue(GroupedTestsResultProtocol.OUTPUT_TRUNCATED in output, output.takeLast(256))
        assertTrue("original length=" in output, output.takeLast(256))
        assertTrue("SHA-256=" in output, output.takeLast(256))
    }
}
