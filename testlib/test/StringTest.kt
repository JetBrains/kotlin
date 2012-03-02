package testString

import std.io.*
import kool.test.*

import junit.framework.*

class StringTest() : TestCase() {

    fun testHeredoc() {
        val a = <<<EOS
line1
line2
EOS
        assertTrue("line1\nline2\n" == a)
    }

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
}
