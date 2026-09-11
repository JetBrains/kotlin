/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.tools

import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class WasmVMTest {
    @Test
    fun `given output that fits the capture limit then no digest is created`() {
        var digestCount = 0
        val capture = BoundedOutputCapture {
            digestCount++
            MessageDigest.getInstance("SHA-256")
        }

        val chunk = CharArray(8 * 1024) { 'x' }
        repeat((4 * 1024 * 1024) / chunk.size) {
            capture.append(chunk, chunk.size)
        }

        assertEquals(0, digestCount)

        capture.append(chunk, chunk.size)

        assertEquals(1, digestCount)
    }

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

    @Test
    fun `given protocol-looking fragments across capture boundaries then only the truncation marker is parsed`() {
        val retainedHalfLength = 2 * 1024 * 1024
        val prefixRecord = "${GroupedTestsResultProtocol.LINE_PREFIX}|prefix|${GroupedTestsResultProtocol.STARTED}||"
        val prefixFragment = prefixRecord.take(prefixRecord.length / 2)
        val prefix = buildString(retainedHalfLength) {
            append(GroupedTestsResultProtocol.BEGIN).append('\n')
            append("x".repeat(retainedHalfLength - length - prefixFragment.length - 1))
            append('\n')
            append(prefixFragment)
        }
        val suffixRecord = "${GroupedTestsResultProtocol.LINE_PREFIX}|suffix|${GroupedTestsResultProtocol.STARTED}||"
        val middle = prefixRecord.drop(prefixFragment.length) + "\nnoise"
        val suffix = suffixRecord + "\n" + "y".repeat(retainedHalfLength - suffixRecord.length - 1)
        val capture = BoundedOutputCapture()

        for (part in listOf(prefix, middle, suffix)) {
            capture.append(part.toCharArray(), part.length)
        }

        val output = capture.toString()
        val (crashedIds, malformedLines) = GroupedTestsResultProtocol.parseMerged(listOf(output))

        assertFalse(prefixFragment in output, output.takeLast(256))
        assertFalse(suffixRecord in output, output.takeLast(256))
        assertEquals(1, malformedLines.size, malformedLines.toString())
        assertTrue(
            malformedLines.single().startsWith(
                "${GroupedTestsResultProtocol.LINE_PREFIX}|${GroupedTestsResultProtocol.OUTPUT_TRUNCATED}|"
            ),
            malformedLines.toString(),
        )
        assertTrue(crashedIds.isEmpty(), crashedIds.toString())
    }

    @Test
    fun `given a single chunk larger than the capture limit then both ends of it are retained`() {
        val capture = BoundedOutputCapture()
        val head = "head marker line\n"
        val tail = "\ntail marker line"
        val chunk = (head + "x".repeat(5 * 1024 * 1024) + tail).toCharArray()

        capture.append(chunk, chunk.size)

        val output = capture.toString()
        assertTrue(output.startsWith(head), output.take(64))
        assertTrue(output.endsWith(tail), output.takeLast(64))
        assertTrue(GroupedTestsResultProtocol.OUTPUT_TRUNCATED in output, output.takeLast(256))
        assertTrue("original length=${chunk.size} chars" in output, output.takeLast(256))
    }

    @Test
    fun `given one line longer than the capture limit then both ends of the line are retained`() {
        val capture = BoundedOutputCapture()
        val head = "head-of-line:"
        val tail = ":end-of-line"
        val chunk = (head + "x".repeat(5 * 1024 * 1024) + tail).toCharArray()

        capture.append(chunk, chunk.size)

        val output = capture.toString()
        assertTrue(output.startsWith(head + "xxx"), output.take(64))
        assertTrue(output.endsWith("xxx$tail"), output.takeLast(64))
        val markerLine = output.lineSequence().single { it.startsWith(GroupedTestsResultProtocol.LINE_PREFIX) }
        assertTrue(GroupedTestsResultProtocol.OUTPUT_TRUNCATED in markerLine, markerLine)
        assertTrue(output.length <= 4 * 1024 * 1024 + 256, output.length.toString())
    }

    @Test
    fun `given a short buffered head and an oversized chunk then the head is completed from the chunk`() {
        val capture = BoundedOutputCapture()
        val buffered = "buffered line\n".toCharArray()
        capture.append(buffered, buffered.size)
        val tail = "\ntail marker line"
        val chunk = ("y".repeat(3 * 1024 * 1024) + "\n" + "z".repeat(2 * 1024 * 1024) + tail).toCharArray()

        capture.append(chunk, chunk.size)

        val output = capture.toString()
        assertTrue(output.startsWith("buffered line\n"), output.take(64))
        assertTrue(output.endsWith(tail), output.takeLast(64))
        assertTrue("original length=${buffered.size + chunk.size} chars" in output, output.takeLast(256))
    }
}
