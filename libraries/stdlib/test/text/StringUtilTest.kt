package test.text

import kotlin.*
import kotlin.test.*
import kotlin.util.*

import junit.framework.*

class StringUtilTest() : TestCase() {

    fun testToRegex() {
        val re = """foo""".toRegex()
        val list = re.split("hellofoobar").toList()
        assertEquals(arrayList("hello", "bar"), list)
    }
}
