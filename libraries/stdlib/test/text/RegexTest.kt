package test.text

import kotlin.text.*

import kotlin.test.*
import org.junit.Test as test

class RegexTest {

    @test fun matchResult() {
        val p = "\\d+".toRegex()
        val input = "123 456 789"

        assertFalse(input.matches(p))
        assertFalse(p.matches(input))

        assertTrue(p in input)

        val first = p.find(input)
        assertTrue(first != null); first!!
        assertEquals("123", first.value)

        val second1 = first.next()!!
        val second2 = first.next()!!

        assertEquals("456", second1.value)
        assertEquals(second1.value, second2.value)

        assertEquals("56", p.find(input, startIndex = 5)?.value)

        val last = second1.next()!!
        assertEquals("789", last.value)

        val noMatch = last.next()
        assertEquals(null, noMatch)
    }

    @test fun matchIgnoreCase() {
        for (input in listOf("ascii", "shrödinger"))
            assertTrue(input.toUpperCase().matches(input.toLowerCase().toRegex(RegexOption.IGNORE_CASE)))
    }

    @test fun matchSequence() {
        val input = "123 456 789"
        val pattern = "\\d+".toRegex()

        val matches = pattern.findAll(input)
        val values = matches.map { it.value }
        val expected = listOf("123", "456", "789")
        assertEquals(expected, values.toList())
        assertEquals(expected, values.toList(), "running match sequence second time")
        assertEquals(expected.drop(1), pattern.findAll(input, startIndex = 3).map { it.value }.toList())

        assertEquals(listOf(0..2, 4..6, 8..10), matches.map { it.range }.toList())
    }

    @test fun matchAllSequence() {
        val input = "test"
        val pattern = ".*".toRegex()
        val matches = pattern.findAll(input).toList()
        assertEquals(input, matches[0].value)
        assertEquals(input, matches.joinToString("") { it.value })
        assertEquals(2, matches.size)
    }

    @test fun matchGroups() {
        val input = "1a 2b 3c"
        val pattern = "(\\d)(\\w)".toRegex()

        val matches = pattern.findAll(input).toList()
        assertTrue(matches.all { it.groups.size == 3 })
        val m1 = matches[0]
        assertEquals("1a", m1.groups[0]?.value)
        assertEquals("1", m1.groups[1]?.value)
        assertEquals("a", m1.groups[2]?.value)

        val m2 = matches[1]
        assertEquals("2", m2.groups[1]?.value)
        assertEquals("b", m2.groups[2]?.value)
    }

    @test fun matchOptionalGroup() {
        val pattern = "(hi)|(bye)".toRegex(RegexOption.IGNORE_CASE)

        val m1 = pattern.find("Hi!")!!
        assertEquals(3, m1.groups.size)
        assertEquals("Hi", m1.groups[1]?.value)
        assertEquals(null, m1.groups[2])

        val m2 = pattern.find("bye...")!!
        assertEquals(3, m2.groups.size)
        assertEquals(null, m2.groups[1])
        assertEquals("bye", m2.groups[2]?.value)
    }

    @test fun matchMultiline() {
        val regex = "^[a-z]*$".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
        val matchedValues = regex.findAll("test\n\nLine").map { it.value }.toList()
        assertEquals(listOf("test", "", "Line"), matchedValues)
    }


    @test fun matchEntire() {
        val regex = "(\\d)(\\w)".toRegex()

        assertNull(regex.matchEntire("1a 2b"))
        assertNotNull(regex.matchEntire("3c")) { m ->
            assertEquals("3c", m.value)
            assertEquals(3, m.groups.size)
            assertEquals(listOf("3c", "3", "c"), m.groups.map { it!!.value })
        }
    }

    @test fun matchEntireLazyQuantor() {
        val regex = "a+b+?".toRegex()
        val input = StringBuilder("aaaabbbb")

        assertEquals("aaaab", regex.find(input)!!.value)
        assertEquals("aaaabbbb", regex.matchEntire(input)!!.value)
    }

    @test fun escapeLiteral() {
        val literal = """[-\/\\^$*+?.()|[\]{}]"""
        assertTrue(Regex.fromLiteral(literal).matches(literal))
        assertTrue(Regex.escape(literal).toRegex().matches(literal))
    }

    @test fun replace() {
        val input = "123-456"
        val pattern = "(\\d+)".toRegex()
        assertEquals("(123)-(456)", pattern.replace(input, "($1)"))

        assertEquals("$&-$&", pattern.replace(input, Regex.escapeReplacement("$&")))
        assertEquals("X-456", pattern.replaceFirst(input, "X"))
    }

    @test fun replaceEvaluator() {
        val input = "/12/456/7890/"
        val pattern = "\\d+".toRegex()
        assertEquals("/2/3/4/", pattern.replace(input, { it.value.length().toString() } ))
    }


    @test fun split() {
        val input = """
         some  ${"\t"}  word
         split
        """.trim()

        assertEquals(listOf("some", "word", "split"), "\\s+".toRegex().split(input))

        assertEquals(listOf("name", "value=5"), "=".toRegex().split("name=value=5", limit = 2))

    }



}