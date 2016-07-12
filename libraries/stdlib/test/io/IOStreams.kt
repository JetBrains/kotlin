package test.io

import org.junit.Test as test
import kotlin.test.*
import java.io.Writer
import java.io.BufferedReader

class IOStreamsTest {
    @test fun testGetStreamOfFile() {
        val tmpFile = createTempFile()
        var writer: Writer? = null
        try {
            writer = tmpFile.outputStream().writer()
            writer.write("Hello, World!")
        } finally {
            writer?.close()
        }
        val act: String?
        var reader: BufferedReader? = null
        try {
            reader = tmpFile.inputStream().reader().buffered()
            act = reader.readLine()
        } finally {
            reader?.close()
        }
        assertEquals("Hello, World!", act)
    }

    @test fun testInputStreamIterator() {
        val x = ByteArray(10) { it.toByte() }

        val result = mutableListOf<Byte>()

        x.inputStream().buffered().use { stream ->
            for(b in stream) {
                result += b
            }
        }

        assertEquals(x.asList(), result)
    }
}
