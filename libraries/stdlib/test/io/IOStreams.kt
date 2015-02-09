package test.io

import org.junit.Test as test
import java.io.Writer
import java.io.BufferedReader
import java.io.File
import kotlinhack.test.assertEquals

class IOStreamsTest {
    test fun testGetStreamOfFile() {
        val tmpFile = createTempFile()
        var writer: Writer? = null
        try {
            writer = tmpFile.outputStream().writer()
            writer!!.write("Hello, World!")
        } finally {
            writer?.close()
        }
        var act: String?
        var reader: BufferedReader? = null
        try {
            reader = tmpFile.inputStream().reader().buffered()
            act = reader!!.readLine()
        } finally {
            reader?.close()
        }
        assertEquals("Hello, World!", act)
    }
}
