package test.text

import kotlin.*
import kotlin.test.*
import org.junit.Test as test

class StringBuilderJVMTest() {
    @test fun getAndSetChar() {
        val sb = StringBuilder("abc")
        sb[1] = 'z'

        assertEquals("azc", sb.toString())
        assertEquals('c', sb[2])
    }
}
