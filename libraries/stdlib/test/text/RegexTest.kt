/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NAMED_ARGUMENTS_NOT_ALLOWED") // for common tests

package test.text

import test.regexSplitUnicodeCodePointHandling
import test.supportsOctalLiteralInRegex
import test.supportsEscapeAnyCharInRegex
import test.BackReferenceHandling
import test.HandlingOption
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

    @Test fun matchEscapeSurrogatePair() {
        if (!supportsEscapeAnyCharInRegex) return

        val regex = "\\\uD83D\uDE00".toRegex()
        assertTrue(regex.matches("\uD83D\uDE00"))
    }

    @Test fun matchEscapeRandomChar() {
        if (!supportsEscapeAnyCharInRegex) return

        val regex = "\\-".toRegex()
        assertTrue(regex.matches("-"))
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

    @Test fun matchNamedGroups() {
        val regex = "\\b(?<city>[A-Za-z\\s]+),\\s(?<state>[A-Z]{2}):\\s(?<areaCode>[0-9]{3})\\b".toRegex()
        val input = "Coordinates: Austin, TX: 123"

        val match = regex.find(input)!!
        assertEquals(listOf("Austin, TX: 123", "Austin", "TX", "123"), match.groupValues)

        val namedGroups = match.groups
        assertEquals(4, namedGroups.size)
        assertEquals("Austin", namedGroups["city"]?.value)
        assertEquals("TX", namedGroups["state"]?.value)
        assertEquals("123", namedGroups["areaCode"]?.value)
    }

    @Test fun matchDuplicateGroupName() {
        // should fail with IllegalArgumentException, but JS fails with SyntaxError
        assertFails { "(?<hi>hi)|(?<hi>bye)".toRegex() }
        assertFails { "(?<first>\\d+)-(?<first>\\d+)".toRegex() }
    }

    @Test fun matchOptionalNamedGroup() {
        "(?<hi>hi)|(?<bye>bye)".toRegex(RegexOption.IGNORE_CASE).let { regex ->
            val hiMatch = regex.find("Hi!")!!
            val hiGroups = hiMatch.groups
            assertEquals(3, hiGroups.size)
            assertEquals("Hi", hiGroups["hi"]?.value)
            assertEquals(null, hiGroups["bye"])
            assertFailsWith<IllegalArgumentException> { hiGroups["hello"] }

            val byeMatch = regex.find("bye...")!!
            val byeGroups = byeMatch.groups
            assertEquals(3, byeGroups.size)
            assertEquals(null, byeGroups["hi"])
            assertEquals("bye", byeGroups["bye"]?.value)
            assertFailsWith<IllegalArgumentException> { byeGroups["goodbye"] }
        }

        "(?<hi>hi)|bye".toRegex(RegexOption.IGNORE_CASE).let { regex ->
            val hiMatch = regex.find("Hi!")!!
            val hiGroups = hiMatch.groups
            assertEquals(2, hiGroups.size)
            assertEquals("Hi", hiGroups["hi"]?.value)
            assertFailsWith<IllegalArgumentException> { hiGroups["bye"] }

            // Named group collection consisting of a single 'null' group value
            val byeMatch = regex.find("bye...")!!
            val byeGroups = byeMatch.groups
            assertEquals(2, byeGroups.size)
            assertEquals(null, byeGroups["hi"])
            assertFailsWith<IllegalArgumentException> { byeGroups["bye"] }
        }
    }

    @Test fun matchWithBackReference() {
        "(\\w+), yes \\1".toRegex().let { regex ->
            val match = regex.find("Do you copy? Sir, yes Sir!")!!
            assertEquals("Sir, yes Sir", match.value)
            assertEquals("Sir", match.groups[1]?.value)

            assertNull(regex.find("Do you copy? Sir, yes I do!"))
        }

        // capture the largest valid group index
        "(\\w+), yes \\12".let { pattern ->
            if (BackReferenceHandling.captureLargestValidIndex) {
                val match = pattern.toRegex().find("Do you copy? Sir, yes Sir2")!!
                assertEquals("Sir, yes Sir2", match.value)
                assertEquals("Sir", match.groups[1]?.value)
            } else {
                // JS throws SyntaxError
                assertFails { pattern.toRegex() }
            }
        }

        // back reference to a group with large index
        "0(1(2(3(4(5(6(7(8(9(A(B(C))))))))\\11))))".toRegex().let { regex ->
            val match = regex.find("0123456789ABCBC")!!
            assertEquals("BC", match.groups[11]?.value)
            assertEquals("56789ABC", match.groups[5]?.value)
            assertEquals("456789ABCBC", match.groups[4]?.value)
        }

        testInvalidBackReference(BackReferenceHandling.nonExistentGroup, pattern = "a(a)\\2")
        testInvalidBackReference(BackReferenceHandling.enclosingGroup, pattern = "a(a\\1)")
        testInvalidBackReference(BackReferenceHandling.notYetDefinedGroup, pattern = "a\\1(a)")

        testInvalidBackReference(BackReferenceHandling.groupZero, pattern = "aa\\0")
        testInvalidBackReference(BackReferenceHandling.groupZero, pattern = "a\\0a")
    }

    @Test fun matchCharWithOctalValue() {
        if (supportsOctalLiteralInRegex) {
            assertEquals("aa", "a\\0141".toRegex().find("aaaa")?.value)
        } else {
            assertFails { "a\\0141".toRegex() }
        }
    }

    @Test fun matchNamedGroupsWithBackReference() {
        "(?<title>\\w+), yes \\k<title>".toRegex().let { regex ->
            val match = regex.find("Do you copy? Sir, yes Sir!")!!
            assertEquals("Sir, yes Sir", match.value)
            assertEquals("Sir", match.groups["title"]?.value)

            assertNull(regex.find("Do you copy? Sir, yes I do!"))
        }

        testInvalidBackReference(BackReferenceHandling.nonExistentNamedGroup, pattern = "a(a)\\k<name>")
        testInvalidBackReference(BackReferenceHandling.enclosingGroup, pattern = "a(?<first>a\\k<first>)")
        testInvalidBackReference(BackReferenceHandling.notYetDefinedNamedGroup, pattern = "a\\k<first>(?<first>a)")
    }

    @Test fun matchNamedGroupCollection() {
        val regex = "(?<hi>hi)".toRegex(RegexOption.IGNORE_CASE)
        val hiMatch = regex.find("Hi!")!!
        val hiGroups = hiMatch.groups as MatchNamedGroupCollection
        assertEquals("Hi", hiGroups["hi"]?.value)
    }

    private fun testInvalidBackReference(option: HandlingOption, pattern: String, input: CharSequence = "aaaa", matchValue: String = "aa") {
        when (option) {
            HandlingOption.IGNORE_BACK_REFERENCE_EXPRESSION ->
                assertEquals(matchValue, pattern.toRegex().find(input)?.value)
            HandlingOption.THROW ->
                // should fail with IllegalArgumentException, but JS fails with SyntaxError
                assertFails { pattern.toRegex() }
            HandlingOption.MATCH_NOTHING ->
                assertNull(pattern.toRegex().find(input))
        }
    }

    @Test fun invalidNamedGroupDeclaration() {
        // should fail with IllegalArgumentException, but JS fails with SyntaxError

        assertFails {
            "(?<".toRegex()
        }
        assertFails {
            "(?<)".toRegex()
        }
        assertFails {
            "(?<name".toRegex()
        }
        assertFails {
            "(?<name)".toRegex()
        }
        assertFails {
            "(?<name>".toRegex()
        }
        assertFails {
            "(?<>\\w+), yes \\k<>".toRegex()
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

        for (r in listOf("$&", "\\$", "\\ $", "$\\")) {
            assertEquals("$r-$r", pattern.replace(input, Regex.escapeReplacement(r)))
        }

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
        val pattern = Regex("(?<first>\\d+)-(?<second>\\d+)")

        "123-456".let { input ->
            assertEquals("(123-456)", pattern.replace(input, "($0)"))
            assertEquals("123+456", pattern.replace(input, "$1+$2"))
            // take the largest legal group number reference
            assertEquals("1230+456", pattern.replace(input, "$10+$2"))
            assertEquals("123+456", pattern.replace(input, "$01+$2"))
            // js refers to named capturing groups with "$<name>" syntax
            assertFailsWith<IllegalArgumentException>("\$<first>+\$<second>") { pattern.replace(input, "\$<first>+\$<second>") }
            assertEquals("123+456", pattern.replace(input, "\${first}+\${second}"))

            // missing trailing '}'
            assertFailsWith<IllegalArgumentException>("\${first+\${second}") { pattern.replace(input, "\${first+\${second}") }
            assertFailsWith<IllegalArgumentException>("\${first}+\${second") { pattern.replace(input, "\${first}+\${second") }

            // non-existent group name
            assertFailsWith<IllegalArgumentException>("\${first}+\${second}+\$third") {
                pattern.replace(input, "\${first}+\${second}+\$third")
            }
        }

        "123-456-789-012".let { input ->
            assertEquals("123/456-789/012", pattern.replace(input, "$1/$2"))
            assertEquals("123/456-789/012", pattern.replace(input, "\${first}/\${second}"))
            assertEquals("123/456-789-012", pattern.replaceFirst(input, "\${first}/\${second}"))
        }
    }

    @Test fun replaceWithNamedOptionalGroups() {
        val regex = "(?<hi>hi)|(?<bye>bye)".toRegex(RegexOption.IGNORE_CASE)

        assertEquals("[Hi, ]gh wall", regex.replace("High wall", "[$1, $2]"))
        assertEquals("[Hi, ]gh wall", regex.replace("High wall", "[\${hi}, \${bye}]"))

        assertEquals("Good[, bye], Mr. Holmes", regex.replace("Goodbye, Mr. Holmes", "[$1, $2]"))
        assertEquals("Good[, bye], Mr. Holmes", regex.replace("Goodbye, Mr. Holmes", "[\${hi}, \${bye}]"))
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

        testSplitEquals(
            if (regexSplitUnicodeCodePointHandling) listOf("", "\uD83D\uDE04", "\uD801", "") else listOf("", "\uD83D", "\uDE04", "\uD801", ""),
            "\uD83D\uDE04\uD801", emptyMatch
        )

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

    @Test fun findAllEmoji() {
        val input = "\uD83D\uDE04\uD801x"
        val regex = ".".toRegex()

        val matches = regex.findAll(input).toList()
        val values = matches.map { it.value }
        val ranges = matches.map { it.range }

        assertEquals(listOf("\uD83D\uDE04", "\uD801", "x"), values)
        assertEquals(listOf(0..1, 2..2, 3..3), ranges)
    }

}
