package test.text

import kotlin.text.*

import kotlin.test.*
import org.junit.Test as test

class RegexTest {

    test fun matchResult() {
        val p = "\\d+".toRegex()
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
        val pattern = "\\d+".toRegex()

        val matches = pattern.matchAll(input)
        val values = matches.map { it.value }
        val expected = listOf("123", "456", "789")
        assertEquals(expected, values.toList())
        assertEquals(expected, values.toList(), "running match sequence second time")

        assertEquals(listOf(0..2, 4..6, 8..10), matches.map { it.range }.toList())
    }

    test fun matchGroups() {
        val input = "1a 2b 3c"
        val pattern = "(\\d)(\\w)".toRegex()

        val matches = pattern.matchAll(input).toList()
        assertTrue(matches.all { it.groups.size() == 3 })
        val m1 = matches[0]
        assertEquals("1a", m1.groups[0]?.value)
        assertEquals("1", m1.groups[1]?.value)
        assertEquals("a", m1.groups[2]?.value)

        val m2 = matches[1]
        assertEquals("2", m2.groups[1]?.value)
        assertEquals("b", m2.groups[2]?.value)
    }

    test fun matchOptionalGroup() {
        val pattern = "(hi)|(bye)".toRegex(RegexOption.IGNORE_CASE)

        val m1 = pattern.match("Hi!")!!
        assertEquals(3, m1.groups.size())
        assertEquals("Hi", m1.groups[1]?.value)
        assertEquals(null, m1.groups[2])

        val m2 = pattern.match("bye...")!!
        assertEquals(3, m2.groups.size())
        assertEquals(null, m2.groups[1])
        assertEquals("bye", m2.groups[2]?.value)
    }

    test fun escapeLiteral() {
        val literal = """[-\/\\^$*+?.()|[\]{}]"""
        assertTrue(Regex.fromLiteral(literal).matches(literal))
        assertTrue(Regex(Regex.escape(literal)).matches(literal))
    }

    test fun replace() {
        val input = "123-456"
        val pattern = "(\\d+)".toRegex()
        assertEquals("(123)-(456)", pattern.replace(input, "($1)"))

    }

    test fun replaceEvaluator() {
        val input = "/12/456/7890/"
        val pattern = "\\d+".toRegex()
        assertEquals("/2/3/4/", pattern.replace(input, { it.value.length().toString() } ))
    }


    test fun split() {
        val input = """
         some  ${"\t"}  word
         split
        """.trim()

        assertEquals(listOf("some", "word", "split"), "\\s+".toRegex().split(input))

        assertEquals(listOf("name", "value=5"), "=".toRegex().split("name=value=5", limit = 2))

    }



}