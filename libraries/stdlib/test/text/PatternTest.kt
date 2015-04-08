package test.text

import kotlin.test.*
import kotlin.text.*
import org.junit.Test as test

class PatternTest {

    test fun matchResult() {
        val p = "\\d+".toPattern()
        val input = "123 456 789"

        val first = p.match(input)
        assertTrue(first != null); first!!
        assertEquals("123", first.value)

        val second1 = first.next()!!
        val second2 = first.next()!!

        assertEquals("456", second1.value)
        assertEquals(second1.value, second2.value)

        val last = second1.next()!!
        assertEquals("789", last.value)

        val noMatch = last.next()
        assertEquals(null, noMatch)
    }

    test fun matchSequence() {
        val input = "123 456 789"
        val pattern = "\\d+".toPattern()

        val matches = pattern.matchAll(input)
        val values = matches.map { it.value }
        val expected = listOf("123", "456", "789")
        assertEquals(expected, values.toList())
        assertEquals(expected, values.toList(), "running match sequence second time")

        assertEquals(listOf(0..2, 4..6, 8..10), matches.map { it.range }.toList())
    }

    test fun matchGroups() {
        val input = "1a 2b 3c"
        val pattern = "(\\d)(\\w)".toPattern()

        val matches = pattern.matchAll(input).toList()
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

    test fun matchOptionalGroup() {
        val pattern = "(hi)|(bye)".toPattern(PatternOption.IGNORE_CASE)

        val m1 = pattern.match("Hi!")!!
        assertEquals(3, m1.groups.size())
        assertEquals("Hi", m1.groups[1]?.value)
        assertEquals(null, m1.groups[2])

        val m2 = pattern.match("bye...")!!
        assertEquals(3, m2.groups.size())
        assertEquals(null, m2.groups[1])
        assertEquals("bye", m2.groups[2]?.value)
    }

}