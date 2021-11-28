/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NAMED_ARGUMENTS_NOT_ALLOWED") // for common tests

package test.text

import test.supportsNamedCapturingGroup
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
        assertNotNull(first)
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

        assertFailsWith<IndexOutOfBoundsException> { p.find(input, -1) }
        assertFailsWith<IndexOutOfBoundsException> { p.find(input, input.length + 1) }
        assertEquals(null, p.find(input, input.length))
    }

    @Test fun matchIgnoreCase() {
        for (input in listOf("ascii", "shrödinger"))
            assertTrue(input.uppercase().matches(input.lowercase().toRegex(RegexOption.IGNORE_CASE)))
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

        assertFailsWith<IndexOutOfBoundsException> { pattern.findAll(input, -1) }
        assertFailsWith<IndexOutOfBoundsException> { pattern.findAll(input, input.length + 1) }
        assertEquals(emptyList(), pattern.findAll(input, input.length).toList())
    }

    @Test fun matchAllSequence() {
        val input = "test"
        val pattern = ".*".toRegex()
        val matches = pattern.findAll(input).toList()
        assertEquals(input, matches[0].value)
        assertEquals(input, matches.joinToString("") { it.value })
        assertEquals(2, matches.size)

        assertEquals("", pattern.findAll(input, input.length).single().value)
        assertEquals("", pattern.find(input, input.length)?.value)
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
            assertNull(m.next())
        }
    }

    @Test fun matchEntireLazyQuantor() {
        val regex = "a+b+?".toRegex()
        val input = StringBuilder("aaaabbbb")

        assertEquals("aaaab", regex.find(input)!!.value)
        assertEquals("aaaabbbb", regex.matchEntire(input)!!.value)
    }

    @Test fun matchEntireNext() {
        val regex = ".*".toRegex()
        val input = "abc"
        val match = regex.matchEntire(input)!!
        assertEquals(input, match.value)
        val next = assertNotNull(match.next())
        assertEquals("", next.value)
        assertEquals(input.length until input.length, next.range)
        assertNull(next.next())
    }

    @Test fun matchAt() {
        val regex = Regex("[a-z][1-5]", RegexOption.IGNORE_CASE)
        val input = "...a4...B1"
        val positions = 0..input.length

        val matchIndices = positions.filter { index -> regex.matchesAt(input, index) }
        assertEquals(listOf(3, 8), matchIndices)
        val reversedIndices = positions.reversed().filter { index -> regex.matchesAt(input, index) }.reversed()
        assertEquals(matchIndices, reversedIndices)

        val matches = positions.mapNotNull { index -> regex.matchAt(input, index)?.let { index to it } }
        assertEquals(matchIndices, matches.map { it.first })
        matches.forEach { (index, match) ->
            assertEquals(index..index + 1, match.range)
            assertEquals(input.substring(match.range), match.value)
        }

        matches.zipWithNext { (_, m1), (_, m2) ->
            assertEquals(m2.range, assertNotNull(m1.next()).range)
        }
        assertNull(matches.last().second.next())

        for (index in listOf(-1, input.length + 1)) {
            assertFailsWith<IndexOutOfBoundsException> { regex.matchAt(input, index) }
            assertFailsWith<IndexOutOfBoundsException> { regex.matchesAt(input, index) }
        }

        val anchoringRegex = Regex("^[a-z]")
        assertFalse(anchoringRegex.matchesAt(input, 3))
        assertNull(anchoringRegex.matchAt(input, 3))

        val lookbehindRegex = Regex("(?<=[a-z])\\d")
        assertTrue(lookbehindRegex.matchesAt(input, 4))
        assertNotNull(lookbehindRegex.matchAt(input, 4)).let { match ->
            assertEquals("4", match.value)
        }
    }

    @Test fun escapeLiteral() {
        val literal = """[-\/\\^$*+?.()|[\]{}]"""
        assertTrue(Regex.fromLiteral(literal).matches(literal))
        assertTrue(Regex.escape(literal).toRegex().matches(literal))
    }

    @Test fun replace() {
        val input = "123-456"
        val pattern = "(\\d+)".toRegex()

        // js String.prototype.replace() inserts a "$"
        assertFailsWith<IllegalArgumentException>("$$") { pattern.replace(input, "$$") }
        // js String.prototype.replace() inserts the matched substring
        assertFailsWith<IllegalArgumentException>("$&") { pattern.replace(input, "$&") }
        // js String.prototype.replace() inserts the portion of the string that precedes the matched substring
        assertFailsWith<IllegalArgumentException>("\$`") { pattern.replace(input, "\$`") }
        // js String.prototype.replace() inserts the portion of the string that follows the matched substring
        assertFailsWith<IllegalArgumentException>("$'") { pattern.replace(input, "$'") }
        // js String.prototype.replace() inserts the replacement string as a literal if it refers to a non-existing capturing group
        assertFailsWith<RuntimeException>("$") { pattern.replace(input, "$") } // should be IAE, however jdk7 throws String IOOBE
        assertFailsWith<IndexOutOfBoundsException>("$2") { pattern.replace(input, "$2") }
        assertFailsWith<IllegalArgumentException>("\$name") { pattern.replace(input, "\$name") }
        assertFailsWith<IllegalArgumentException>("\${name}") { pattern.replace(input, "\${name}") }
        assertFailsWith<IllegalArgumentException>("$-") { pattern.replace(input, "$-") }

        // inserts "$" literally
        assertEquals("$-$", pattern.replace(input, "\\$"))
        // inserts the matched substring
        assertEquals("(123)-(456)", pattern.replace(input, "($0)"))
        // inserts the first captured group
        assertEquals("(123)-(456)", pattern.replace(input, "($1)"))

        assertEquals("$&-$&", pattern.replace(input, Regex.escapeReplacement("$&")))
        assertEquals("X-456", pattern.replaceFirst(input, "X"))

        val longInput = "0123456789ABC"
        val longPattern = "0(1(2(3(4(5(6(7(8(9(A(B(C))))))))))))".toRegex()
        for (groupIndex in 0..12) {
            assertEquals(longInput.substring(groupIndex), longPattern.replace(longInput, "$$groupIndex"))
        }
        assertEquals(longInput.substring(1) + "3", longPattern.replace(longInput, "$13"))

        // KT-38000
        assertEquals("""\,""", ",".replace("([,])".toRegex(), """\\$1"""))
        // KT-28378
        assertEquals("$ 2", "2".replace(Regex("(.+)"), "\\$ $1"))
        assertEquals("$2", "2".replace(Regex("(.+)"), "\\$$1"))
        assertFailsWith<IllegalArgumentException> { "2".replace(Regex("(.+)"), "$ $1") }
    }

    @Test fun replaceWithNamedGroups() {
        if (!supportsNamedCapturingGroup) {
            assertFails {
                val pattern = Regex("(?<first>\\d+)-(?<second>\\d+)")
                pattern.replace("123-456", "\${first}+\${second}")
            }
            return
        }

        val pattern = Regex("(?<first>\\d+)-(?<second>\\d+)")

        "123-456".let { input ->
            assertEquals("(123-456)", pattern.replace(input, "($0)"))
            assertEquals("123+456", pattern.replace(input, "$1+$2"))
            // take largest legal group number reference
            assertEquals("1230+456", pattern.replace(input, "$10+$2"))
            assertEquals("123+456", pattern.replace(input, "$01+$2"))
            // js refers to named capturing groups with "$<name>" syntax
            assertFailsWith<IllegalArgumentException>("\$<first>+\$<second>") { pattern.replace(input, "\$<first>+\$<second>") }
            assertEquals("123+456", pattern.replace(input, "\${first}+\${second}"))

            // missing trailing '}'
            assertFailsWith<IllegalArgumentException>("\${first+\${second}") { pattern.replace(input, "\${first+\${second}") }
            assertFailsWith<IllegalArgumentException>("\${first}+\${second") { pattern.replace(input, "\${first}+\${second") }
        }

        "123-456-789-012".let { input ->
            assertEquals("123/456-789/012", pattern.replace(input, "$1/$2"))
            assertEquals("123/456-789/012", pattern.replace(input, "\${first}/\${second}"))
            assertEquals("123/456-789-012", pattern.replaceFirst(input, "\${first}/\${second}"))
        }
    }

    @Test fun replaceEvaluator() {
        val input = "/12/456/7890/"
        val pattern = "\\d+".toRegex()
        assertEquals("/2/3/4/", pattern.replace(input, { it.value.length.toString() }))
    }

    private fun testSplitEquals(expected: List<String>, input: CharSequence, regex: Regex, limit: Int = 0) {
        assertEquals(expected, input.split(regex, limit))
        assertEquals(expected, regex.split(input, limit))

        listOf(
            input.splitToSequence(regex, limit),
            regex.splitToSequence(input, limit)
        ).forEach { sequence ->
            assertEquals(expected, sequence.toList())
            assertEquals(expected, sequence.toList()) // assert multiple iterations over the same sequence succeed
        }
    }

    @Test fun split() {
        val input = """
         some  ${"\t"}  word
         split
        """.trim()

        testSplitEquals(listOf("some", "word", "split"), input, "\\s+".toRegex())

        testSplitEquals(listOf("name", "value=5"), "name=value=5", "=".toRegex(), limit = 2)

    }

    @Test fun splitByEmptyMatch() {
        val input = "test"

        val emptyMatch = "".toRegex()

        testSplitEquals(listOf("", "t", "e", "s", "t", ""), input, emptyMatch)
        testSplitEquals(listOf("", "t", "est"), input, emptyMatch, limit = 3)

        testSplitEquals("".split(""), "", emptyMatch)

        val emptyMatchBeforeT = "(?=t)".toRegex()

        testSplitEquals(listOf("", "tes", "t"), input, emptyMatchBeforeT)
        testSplitEquals(listOf("", "test"), input, emptyMatchBeforeT, limit = 2)

        testSplitEquals(listOf("", "tee"), "tee", emptyMatchBeforeT)
    }

    @Test fun splitByNoMatch() {
        val input = "test"
        val xMatch = "x".toRegex()

        for (limit in 0..2) {
            testSplitEquals(listOf(input), input, xMatch, limit)
        }
    }

    @Test fun splitWithLimitOne() {
        val input = "/12/456/7890/"
        val regex = "\\d+".toRegex()

        testSplitEquals(listOf(input), input, regex, limit = 1)
    }

    @Test fun findAllAndSplitToSequence() {
        val input = "a12bc456def7890ghij"
        val regex = "\\d+".toRegex()

        val matches = regex.findAll(input).map { it.value }.iterator()
        val splits = regex.splitToSequence(input).iterator()

        assertEquals("12", matches.next())
        assertEquals("a", splits.next())
        assertEquals("456", matches.next())
        assertEquals("bc", splits.next())
        assertEquals("def", splits.next())
        assertEquals("ghij", splits.next())
        assertEquals("7890", matches.next())

        assertFailsWith<NoSuchElementException> { matches.next() }
        assertFailsWith<NoSuchElementException> { splits.next() }
    }

}
