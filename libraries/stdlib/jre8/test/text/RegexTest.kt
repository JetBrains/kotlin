package kotlin.text.test

import org.junit.Test
import kotlin.test.*


class RegexTest {
    @Test fun namedGroups() {
        val input = "1a 2b 3c"
        val regex = "(?<num>\\d)(?<liter>\\w)".toRegex()

        val matches = regex.findAll(input).toList()
        assertTrue(matches.all { it.groups.size == 3 })
        val m1 = matches[0]

        assertEquals("1", m1.groups["num"]?.value)
        assertEquals(0..0, m1.groups["num"]?.range)
        assertEquals("a", m1.groups["liter"]?.value)
        assertEquals(1..1, m1.groups["liter"]?.range)

        val m2 = matches[1]
        assertEquals("2", m2.groups["num"]?.value)
        assertEquals(3..3, m2.groups["num"]?.range)
        assertEquals("b", m2.groups["liter"]?.value)
        assertEquals(4..4, m2.groups["liter"]?.range)
    }
}