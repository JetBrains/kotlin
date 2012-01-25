package testString

import std.io.*
import stdhack.test.*

import junit.framework.*

class StringTest() : TestCase() {
    fun testStringIterator() {
        var sum = 0
        for(c in "239")
            sum += (c.int - '0'.int)
        assertTrue(sum == 14)
    }

    fun testStringBuilderIterator() {
        var sum = 0
        val sb = StringBuilder()
        for(c in "239")
            sb.append(c)

        println(sb)

        for(c in sb)
            sum += (c.int - '0'.int)
        assertTrue(sum == 14)
    }
}
