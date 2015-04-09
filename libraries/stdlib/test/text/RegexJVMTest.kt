package test.text

import kotlin.test.*
import kotlin.text.*
import org.junit.Test as test


class RegexJVMTest {

    test fun matchGroups() {
        val input = "1a 2b 3c"
        val regex = "(\\d)(\\w)".toRegex()

        val matches = regex.matchAll(input).toList()
        assertTrue(matches.all { it.groups.size() == 3 })
        val m1 = matches[0]
        assertEquals("1a", m1.groups[0]?.value)
        assertEquals(0..1, m1.groups[0]?.range)
        assertEquals("1", m1.groups[1]?.value)
        assertEquals(0..0, m1.groups[1]?.range)
        assertEquals("a", m1.groups[2]?.value)
        assertEquals(1..1, m1.groups[2]?.range)

        val m2 = matches[1]
        assertEquals("2", m2.groups[1]?.value)
        assertEquals(3..3, m2.groups[1]?.range)
        assertEquals("b", m2.groups[2]?.value)
        assertEquals(4..4, m2.groups[2]?.range)
    }
}
