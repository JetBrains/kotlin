package test.string

import kotlin.io.*
import kotlin.test.*

import junit.framework.*

class StringTest() : TestCase() {
    fun testStringIterator() {
        var sum = 0
        for(c in "239")
            sum += (c.toInt() - '0'.toInt())
        assertTrue(sum == 14)
    }

    fun testStringBuilderIterator() {
        var sum = 0
        val sb = StringBuilder()
        for(c in "239")
            sb.append(c)

        println(sb)

        for(c in sb)
            sum += (c.toInt() - '0'.toInt())
        assertTrue(sum == 14)
    }

    fun testOrEmpty() {
        val s: String? = "hey"
        val ns: String? = null

        assertEquals("hey", s.orEmpty())
        assertEquals("", ns.orEmpty())
    }

    fun testToShort() {
        assertEquals(77.toShort(), "77".toShort())
    }

    fun testToInt() {
        assertEquals(77, "77".toInt())
    }

    fun testToLong() {
        assertEquals(77.toLong(), "77".toLong())
    }

}
