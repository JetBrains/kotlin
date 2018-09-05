/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NAMED_ARGUMENTS_NOT_ALLOWED") // for common tests
package test.text

import kotlin.text.*

import kotlin.test.*

class RegexTest {

    @Test fun properties() {
        val pattern = "\\s+$"
        val regex1 = Regex(pattern, RegexOption.IGNORE_CASE)
        assertEquals(pattern, regex1.pattern)
        assertEquals(setOf(RegexOption.IGNORE_CASE), regex1.options)

        val options2 = setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
        val regex2 = Regex(pattern, options2)
        assertEquals(options2, regex2.options)
    }

    @Test fun matchResult() {
        val p = "\\d+".toRegex()
        val input = "123 456 789"

        assertFalse(input matches p)
        assertFalse(p matches input)

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

    @Test fun matchIgnoreCase() {
        for (input in listOf("ascii", "shrödinger"))
            assertTrue(input.toUpperCase().matches(input.toLowerCase().toRegex(RegexOption.IGNORE_CASE)))
    }

    @Test fun matchSequence() {
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

    @Test fun matchAllSequence() {
        val input = "test"
        val pattern = ".*".toRegex()
        val matches = pattern.findAll(input).toList()
        assertEquals(input, matches[0].value)
        assertEquals(input, matches.joinToString("") { it.value })
        assertEquals(2, matches.size)
    }

    @Test fun matchGroups() {
        val input = "1a 2b 3c"
        val pattern = "(\\d)(\\w)".toRegex()

        val matches = pattern.findAll(input).toList()
        assertTrue(matches.all { it.groups.size == 3 })

        matches[0].let { m ->
            assertEquals("1a", m.groups[0]?.value)
            assertEquals("1", m.groups[1]?.value)
            assertEquals("a", m.groups[2]?.value)

            assertEquals(listOf("1a", "1", "a"), m.groupValues)

            val (g1, g2) = m.destructured
            assertEquals("1", g1)
            assertEquals("a", g2)
            assertEquals(listOf("1", "a"), m.destructured.toList())
        }

        matches[1].let { m ->
            assertEquals("2b", m.groups[0]?.value)
            assertEquals("2", m.groups[1]?.value)
            assertEquals("b", m.groups[2]?.value)

            assertEquals(listOf("2b", "2", "b"), m.groupValues)

            val (g1, g2) = m.destructured
            assertEquals("2", g1)
            assertEquals("b", g2)
            assertEquals(listOf("2", "b"), m.destructured.toList())
        }
    }

    @Test fun matchOptionalGroup() {
        val pattern = "(hi)|(bye)".toRegex(RegexOption.IGNORE_CASE)

        pattern.find("Hi!")!!.let { m ->
            assertEquals(3, m.groups.size)
            assertEquals("Hi", m.groups[1]?.value)
            assertEquals(null, m.groups[2])

            assertEquals(listOf("Hi", "Hi", ""), m.groupValues)

            val (g1, g2) = m.destructured
            assertEquals("Hi", g1)
            assertEquals("", g2)
            assertEquals(listOf("Hi", ""), m.destructured.toList())
        }

        pattern.find("bye...")!!.let { m ->
            assertEquals(3, m.groups.size)
            assertEquals(null, m.groups[1])
            assertEquals("bye", m.groups[2]?.value)

            assertEquals(listOf("bye", "", "bye"), m.groupValues)

            val (g1, g2) = m.destructured
            assertEquals("", g1)
            assertEquals("bye", g2)
            assertEquals(listOf("", "bye"), m.destructured.toList())
        }
    }

    @Test fun matchMultiline() {
        val regex = "^[a-z]*$".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
        val matchedValues = regex.findAll("test\n\nLine").map { it.value }.toList()
        assertEquals(listOf("test", "", "Line"), matchedValues)
    }


    @Test fun matchEntire() {
        val regex = "(\\d)(\\w)".toRegex()

        assertNull(regex.matchEntire("1a 2b"))
        assertNotNull(regex.matchEntire("3c")) { m ->
            assertEquals("3c", m.value)
            assertEquals(3, m.groups.size)
            assertEquals(listOf("3c", "3", "c"), m.groups.map { it!!.value })
        }
    }

    @Test fun matchEntireLazyQuantor() {
        val regex = "a+b+?".toRegex()
        val input = StringBuilder("aaaabbbb")

        assertEquals("aaaab", regex.find(input)!!.value)
        assertEquals("aaaabbbb", regex.matchEntire(input)!!.value)
    }

    @Test fun escapeLiteral() {
        val literal = """[-\/\\^$*+?.()|[\]{}]"""
        assertTrue(Regex.fromLiteral(literal).matches(literal))
        assertTrue(Regex.escape(literal).toRegex().matches(literal))
    }

    @Test fun replace() {
        val input = "123-456"
        val pattern = "(\\d+)".toRegex()
        assertEquals("(123)-(456)", pattern.replace(input, "($1)"))

        assertEquals("$&-$&", pattern.replace(input, Regex.escapeReplacement("$&")))
        assertEquals("X-456", pattern.replaceFirst(input, "X"))
    }

    @Test fun replaceEvaluator() {
        val input = "/12/456/7890/"
        val pattern = "\\d+".toRegex()
        assertEquals("/2/3/4/", pattern.replace(input, { it.value.length.toString() }))
    }


    @Test fun split() {
        val input = """
         some  ${"\t"}  word
         split
        """.trim()

        assertEquals(listOf("some", "word", "split"), "\\s+".toRegex().split(input))

        assertEquals(listOf("name", "value=5"), "=".toRegex().split("name=value=5", limit = 2))

    }

    @Test fun splitByEmptyMatch() {
        val input = "test"

        val emptyMatch = "".toRegex()

        assertEquals(input.split(""), input.split(emptyMatch))
        assertEquals(input.split("", limit = 3), input.split(emptyMatch, limit = 3))

        assertEquals("".split(""), "".split(emptyMatch))

        val emptyMatchBeforeT = "(?=t)".toRegex()

        assertEquals(listOf("", "tes", "t"), input.split(emptyMatchBeforeT))
        assertEquals(listOf("", "test"), input.split(emptyMatchBeforeT, limit = 2))

        assertEquals(listOf("", "tee"), "tee".split(emptyMatchBeforeT))
    }



}