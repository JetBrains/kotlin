/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Forwards the standard output of the test data manager process to [delegate], holding back a line that starts with
 * `<ijLog>` until the line is complete.
 *
 * Gradle reads the output of a forked process in chunks of 8 KiB and flushes each chunk on its own, so a longer line
 * reaches IDEA in pieces. IDEA takes output for a test event only when one piece starts with `<ijLog>` and ends with
 * `</ijLog>`: a split event, typically a failure with its stack trace, is printed as plain text, and the test stays
 * running in the test tree. A line written at once inside the Gradle daemon is split only above 1 MiB.
 *
 * Other output passes through on every flush, as before: once a part of a line is written, the rest of it is no event.
 *
 * [close] writes the rest but keeps [delegate] open: it is the standard output of the daemon.
 */
internal class IjLogLineOutputStream(private val delegate: OutputStream) : OutputStream() {
    private val line = LineBuffer()
    private var isLineStarted = false

    override fun write(b: Int) {
        line.write(b)
        if (b == '\n'.code) endLine()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        var lineStart = off
        for (i in off until off + len) {
            if (b[i] == '\n'.code.toByte()) {
                line.write(b, lineStart, i + 1 - lineStart)
                endLine()
                lineStart = i + 1
            }
        }
        line.write(b, lineStart, off + len - lineStart)
    }

    override fun flush() {
        if (line.size() > 0 && (isLineStarted || !line.mayBeIjLog())) {
            writeBuffered()
            isLineStarted = true
        }
        delegate.flush()
    }

    override fun close() {
        writeBuffered()
        delegate.flush()
    }

    private fun endLine() {
        writeBuffered()
        isLineStarted = false
    }

    private fun writeBuffered() {
        if (line.size() == 0) return
        line.writeTo(delegate)
        line.reset()
    }

    private class LineBuffer : ByteArrayOutputStream() {
        /** Whether the buffered text starts with `<ijLog>` or may still become such a line. */
        fun mayBeIjLog(): Boolean = (0 until minOf(count, IJ_LOG_START.size)).all { buf[it] == IJ_LOG_START[it] }
    }

    private companion object {
        val IJ_LOG_START = "<ijLog>".toByteArray()
    }
}
