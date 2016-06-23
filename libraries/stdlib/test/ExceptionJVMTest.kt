package test.exceptions

import kotlin.test.*
import test.collections.assertArrayNotSameButEquals

import org.junit.Test  as test
import java.io.PrintWriter
import java.io.*

class ExceptionJVMTest {

    @test fun printStackTraceOnRuntimeException() {
        assertPrintStackTrace(RuntimeException("Crikey!"))
        assertPrintStackTraceStream(RuntimeException("Crikey2"))
    }

    @test fun printStackTraceOnError() {
        assertPrintStackTrace(Error("Oh dear"))
        assertPrintStackTraceStream(Error("Oh dear2"))
    }


    fun assertPrintStackTrace(t: Throwable) {
        val buffer = StringWriter()
        val writer = PrintWriter(buffer)
        t.printStackTrace(writer)
        println(buffer)
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
        assertTrue(bytes.size > 10)
    }

    @test fun changeStackTrace() {
        val exception = RuntimeException("Fail")
        var stackTrace = exception.stackTrace
        stackTrace = stackTrace.dropLast(1).toTypedArray()
        exception.stackTrace = stackTrace
        assertArrayNotSameButEquals(stackTrace, exception.stackTrace)
    }
}
