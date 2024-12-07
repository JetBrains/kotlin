/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.exceptions

import test.collections.assertArrayNotSameButEquals
import java.io.*
import java.nio.charset.Charset
import kotlin.test.*

class ExceptionJVMTest {

    @Test
    fun printStackTraceOnRuntimeException() {
        assertPrintStackTrace(RuntimeException("Crikey!"))
        assertPrintStackTraceStream(RuntimeException("Crikey2"))
        assertToStringWithTrace(RuntimeException("ToString"))
    }

    @Test
    fun printStackTraceOnError() {
        assertPrintStackTrace(Error("Oh dear"))
        assertPrintStackTraceStream(Error("Oh dear2"))
        assertToStringWithTrace(Error("ToString"))
    }


    fun assertPrintStackTrace(t: Throwable) {
        val buffer = StringWriter()
        val writer = PrintWriter(buffer)
        t.printStackTrace(writer)
        comparePrintedThrowableResult(t, buffer.buffer)
    }

    fun assertPrintStackTraceStream(t: Throwable) {
        val byteBuffer = ByteArrayOutputStream()

        PrintStream(byteBuffer).use {
            t.printStackTrace(it)
        }

        val stream = PrintStream(byteBuffer)
        stream.use {
            t.printStackTrace(stream)
        }

        val bytes = assertNotNull(byteBuffer.toByteArray())
        val content = bytes.toString(Charset.defaultCharset())
        comparePrintedThrowableResult(t, content)
    }

    fun assertToStringWithTrace(t: Throwable) {
        val content = t.stackTraceToString()
        comparePrintedThrowableResult(t, content)
    }

    private fun comparePrintedThrowableResult(throwable: Throwable, printedThrowable: CharSequence) {
        val stackTrace = throwable.stackTrace
        val lines = printedThrowable.lines()
        assertEquals(throwable.toString(), lines[0])
        stackTrace.forEachIndexed { index, frame ->
            assertTrue(lines.any { frame.toString() in it }, "frame at index $index is not found in the printed message")
        }
    }

    @Test
    fun changeStackTrace() {
        val exception = RuntimeException("Fail")
        var stackTrace = exception.stackTrace
        stackTrace = stackTrace.dropLast(1).toTypedArray()
        exception.stackTrace = stackTrace
        assertArrayNotSameButEquals(stackTrace, exception.stackTrace)
    }

    @Test
    fun addSuppressedDoesNotThrow() {
        val e1 = Throwable()
        val e2 = Exception("Suppressed")

        e1.addSuppressed(e2)
    }

    @Test
    fun addSuppressedSelfDoesNotThrow() {
        val e1 = Throwable()
        e1.addSuppressed(e1) // should not throw, extension hides member
    }

    @Test
    fun addSuppressedWorksThroughExtension() {
        val e1 = Throwable()
        val e2 = Exception("Suppressed")

        assertTrue(e1.suppressedExceptions.isEmpty())
        e1.addSuppressed(e2)

        assertSame(e2, e1.suppressed.singleOrNull())
        assertSame(e2, e1.suppressedExceptions.singleOrNull())
    }

    @Test
    fun circularCauseStackTrace() {
        val e1 = Exception("cause")
        val e2 = Error("induced", e1)
        e1.initCause(e2)
        assertSame(e1, e2.cause)
        assertSame(e2, e1.cause)

        val trace = e2.stackTraceToString()
        assertTrue("CIRCULAR REFERENCE" in trace, trace)
    }
}
