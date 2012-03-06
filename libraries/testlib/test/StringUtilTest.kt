package test.string

import kotlin.*
import kotlin.test.*
import kotlin.util.*

import junit.framework.*

class StringUtilTest() : TestCase() {

    fun testToRegex() {
        val re = """foo""".toRegex()
        val list = re.split("hellofoobar").filterNulls()
        assertEquals(arrayList("hello", "bar"), list)
    }
}
