/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.exceptions

import kotlin.test.*
import test.collections.assertArrayNotSameButEquals

import java.io.PrintWriter
import java.io.*
import java.nio.charset.Charset

class ExceptionJVMTest {

    @Test fun printStackTraceOnRuntimeException() {
        assertPrintStackTrace(RuntimeException("Crikey!"))
        assertPrintStackTraceStream(RuntimeException("Crikey2"))
    }

    @Test fun printStackTraceOnError() {
        assertPrintStackTrace(Error("Oh dear"))
        assertPrintStackTraceStream(Error("Oh dear2"))
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

    private fun comparePrintedThrowableResult(throwable: Throwable, printedThrowable: CharSequence) {
        val stackTrace = throwable.stackTrace
        val lines = printedThrowable.lines()
        assertEquals(throwable.toString(), lines[0])
        stackTrace.forEachIndexed { index, frame ->
            assertTrue(lines.any { frame.toString() in it }, "frame at index $index is not found in the printed message")
        }
    }

    @Test fun changeStackTrace() {
        val exception = RuntimeException("Fail")
        var stackTrace = exception.stackTrace
        stackTrace = stackTrace.dropLast(1).toTypedArray()
        exception.stackTrace = stackTrace
        assertArrayNotSameButEquals(stackTrace, exception.stackTrace)
    }

    @Test fun addSuppressedDoesNotThrow() {
        val e1 = Throwable()
        val e2 = Exception("Suppressed")

        e1.addSuppressed(e2)
    }
}
