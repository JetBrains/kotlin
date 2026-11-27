/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class IjLogLineOutputStreamTest {
    @Test
    fun `an event split across reads is written at once`() {
        assertForwarded(
            reads = listOf("<ijLog>first ", "second ", "third</ijLog>\n"),
            expected = """
                flush
                flush
                write <ijLog>first second third</ijLog>\n
                flush
            """,
        )
    }

    @Test
    fun `an event split inside its opening tag is written at once`() {
        assertForwarded(
            reads = listOf("<ij", "Log>event</ijLog>\n"),
            expected = """
                flush
                write <ijLog>event</ijLog>\n
                flush
            """,
        )
    }

    @Test
    fun `lines of one read are written one by one`() {
        assertForwarded(
            reads = listOf("output\n<ijLog>event</ijLog>\n<ijLog>next", "</ijLog>\n"),
            expected = """
                write output\n
                write <ijLog>event</ijLog>\n
                flush
                write <ijLog>next</ijLog>\n
                flush
            """,
        )
    }

    @Test
    fun `other output is written on every flush`() {
        assertForwarded(
            reads = listOf("progress", " 50%", " 100%\n", "<"),
            expected = """
                write progress
                flush
                write  50%
                flush
                write  100%\n
                flush
                flush
            """,
        )
    }

    @Test
    fun `the rest of a written line is no event`() {
        assertForwarded(
            reads = listOf("output:", "<ijLog>", " more"),
            expected = """
                write output:
                flush
                write <ijLog>
                flush
                write  more
                flush
            """,
        )
    }

    @Test
    fun `close writes the rest and keeps the delegate open`() {
        assertForwarded(
            reads = listOf("<ijLog>unterminated"),
            close = true,
            expected = """
                flush
                write <ijLog>unterminated
                flush
            """,
        )
    }

    /** Feeds [reads] the way Gradle forwards the output of a process: each read is written and flushed. */
    private fun assertForwarded(reads: List<String>, expected: String, close: Boolean = false) {
        val delegate = RecordingOutputStream()
        val stream = IjLogLineOutputStream(delegate)
        for (read in reads) {
            stream.write(read.toByteArray())
            stream.flush()
        }
        if (close) stream.close()
        assertEquals(expected.trimIndent(), delegate.calls.joinToString("\n"))
    }

    private class RecordingOutputStream : OutputStream() {
        val calls = mutableListOf<String>()

        override fun write(b: Int) {
            write(byteArrayOf(b.toByte()), 0, 1)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            calls += "write " + String(b, off, len).replace("\n", "\\n")
        }

        override fun flush() {
            calls += "flush"
        }

        override fun close() {
            calls += "close"
        }
    }
}
