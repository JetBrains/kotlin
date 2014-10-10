package test.text

import kotlin.*
import kotlin.test.*
import org.junit.Test as test

class StringUtilTest() {
    test fun toRegex() {
        val re = """foo""".toRegex()
        val list = re.split("hellofoobar").toList()
        assertEquals(listOf("hello", "bar"), list)
    }
}
