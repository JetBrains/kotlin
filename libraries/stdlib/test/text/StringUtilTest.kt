package test.text

import kotlin.*
import kotlin.test.*
import org.junit.Test as test

class StringUtilTest() {
    test fun toPattern() {
        val re = """foo""".toPattern()
        val list = re.split("hellofoobar").toList()
        assertEquals(listOf("hello", "bar"), list)
    }
}
