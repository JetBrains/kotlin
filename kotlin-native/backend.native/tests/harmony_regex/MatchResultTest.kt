/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package test.text.harmony_regex

import kotlin.text.*
import kotlin.test.*

class MatchResultTest {

    fun assertTrue(msg: String, value: Boolean) = assertTrue(value, msg)
    fun assertFalse(msg: String, value: Boolean) = assertFalse(value, msg)

    internal var testPatterns = arrayOf("(a|b)*abb", "(1*2*3*4*)*567", "(a|b|c|d)*aab", "(1|2|3|4|5|6|7|8|9|0)(1|2|3|4|5|6|7|8|9|0)*", "(abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ)*", "(a|b)*(a|b)*A(a|b)*lice.*", "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)(a|b|c|d|e|f|g|h|" + "i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)*(1|2|3|4|5|6|7|8|9|0)*|while|for|struct|if|do")

    internal var groupPatterns = arrayOf("(a|b)*aabb", "((a)|b)*aabb", "((a|b)*)a(abb)", "(((a)|(b))*)aabb", "(((a)|(b))*)aa(b)b", "(((a)|(b))*)a(a(b)b)")

    @Test fun testReplaceAll() {
        val input = "aabfooaabfooabfoob"
        val pattern = "a*b"
        val regex = Regex(pattern)

        assertEquals("-foo-foo-foo-", regex.replace(input, "-"))
    }

    @Test fun testReplaceFirst() {
        val input = "zzzdogzzzdogzzz"
        val pattern = "dog"
        val regex = Regex(pattern)

        assertEquals("zzzcatzzzdogzzz", regex.replaceFirst(input, "cat"))
    }

    /*
     * Class under test for String group(int)
     */
    @Test fun testGroupint() {
        val positiveTestString = "ababababbaaabb"

        // test IndexOutOfBoundsException
        // //
        for (i in groupPatterns.indices) {
            val regex = Regex(groupPatterns[i])
            val result = regex.matchEntire(positiveTestString)!!
            try {
                // groupPattern <index + 1> equals to number of groups
                // of the specified pattern
                // //
                result.groups[i + 2]
                fail("IndexOutBoundsException expected")
                result.groups[i + 100]
                fail("IndexOutBoundsException expected")
                result.groups[-1]
                fail("IndexOutBoundsException expected")
                result.groups[-100]
                fail("IndexOutBoundsException expected")
            } catch (e: IndexOutOfBoundsException) {
            }
        }

        val groupResults = arrayOf(
                arrayOf("a"),
                arrayOf("a", "a"),
                arrayOf("ababababba", "a", "abb"),
                arrayOf("ababababba", "a", "a", "b"),
                arrayOf("ababababba", "a", "a", "b", "b"),
                arrayOf("ababababba", "a", "a", "b", "abb", "b")
        )

        for (i in groupPatterns.indices) {
            val regex = Regex(groupPatterns[i])
            val result = regex.matchEntire(positiveTestString)!!
            for (j in 0..groupResults[i].size - 1) {
                assertEquals(groupResults[i][j], result.groupValues[j + 1], "i: $i j: $j")
            }
        }

    }

    @Test fun testGroup() {
        val positiveTestString = "ababababbaaabb"
        val negativeTestString = "gjhfgdsjfhgcbv"
        for (element in groupPatterns) {
            val regex = Regex(element)
            val result = regex.matchEntire(positiveTestString)!!
            assertEquals(positiveTestString, result.groupValues[0])
            assertEquals(positiveTestString, result.groups[0]!!.value)
            assertEquals(0 until positiveTestString.length, result.groups[0]!!.range)
        }

        for (element in groupPatterns) {
            val regex = Regex(element)
            val result = regex.matchEntire(negativeTestString)
            assertEquals(result, null)
        }
    }

    @Test fun testGroupPossessive() {
        val regex = Regex("((a)|(b))++c")
        assertEquals("a", regex.matchEntire("aac")!!.groupValues[1])
    }

    @Test fun testMatchesMisc() {
        val posSeq = arrayOf(
                arrayOf("abb", "ababb", "abababbababb", "abababbababbabababbbbbabb"),
                arrayOf("213567", "12324567", "1234567", "213213567", "21312312312567", "444444567"),
                arrayOf("abcdaab", "aab", "abaab", "cdaab", "acbdadcbaab"),
                arrayOf("213234567", "3458", "0987654", "7689546432", "0398576", "98432", "5"),
                arrayOf("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"),
                arrayOf("ababbaAabababblice", "ababbaAliceababab", "ababbAabliceaaa", "abbbAbbbliceaaa", "Alice"),
                arrayOf("a123", "bnxnvgds156", "for", "while", "if", "struct"))

        for (i in testPatterns.indices) {
            val regex = Regex(testPatterns[i])
            for (j in 0..posSeq[i].size - 1) {
                assertTrue("Incorrect match: " + testPatterns[i] + " vs " + posSeq[i][j], regex.matches(posSeq[i][j]))
            }
        }
    }

    @Test fun testMatchesQuantifiers() {
        val testPatternsSingles = arrayOf("a{5}", "a{2,4}", "a{3,}")
        val testPatternsMultiple = arrayOf("((a)|(b)){1,2}abb", "((a)|(b)){2,4}", "((a)|(b)){3,}")

        val stringSingles = arrayOf(
                arrayOf("aaaaa", "aaa"),
                arrayOf("aa", "a", "aaa", "aaaaaa", "aaaa", "aaaaa"),
                arrayOf("aaa", "a", "aaaa", "aa")
        )

        val stringMultiples = arrayOf(
                arrayOf("ababb", "aba"),
                arrayOf("ab", "b", "bab", "ababa", "abba", "abababbb"),
                arrayOf("aba", "b", "abaa", "ba")
        )

        for (i in testPatternsSingles.indices) {
            val regex = Regex(testPatternsSingles[i])
            for (j in 0..stringSingles.size / 2 - 1) {
                assertTrue("Match expected, but failed: " + regex.pattern + " : " + stringSingles[i][j],
                        regex.matches(stringSingles[i][j * 2])
                )
                assertFalse("Match failure expected, but match succeed: " + regex.pattern + " : " + stringSingles[i][j * 2 + 1],
                        regex.matches(stringSingles[i][j * 2 + 1])
                )
            }
        }

        for (i in testPatternsMultiple.indices) {
            val regex = Regex(testPatternsMultiple[i])
            for (j in 0..stringMultiples.size / 2 - 1) {
                assertTrue("Match expected, but failed: " + regex.pattern + " : " + stringMultiples[i][j],
                        regex.matches(stringMultiples[i][j * 2])
                )
                assertFalse("Match failure expected, but match succeed: " + regex.pattern + " : " + stringMultiples[i][j * 2 + 1],
                        regex.matches(stringMultiples[i][j * 2 + 1])
                )
            }
        }

        // Test for the optimized '.+' quantifier node.
        assertFalse(Regex(".+abc").matches("abc"))
        assertFalse(Regex(".+\nabc", RegexOption.DOT_MATCHES_ALL).matches("\nabc"))
        assertFalse(Regex(".+").matches(""))
        assertFalse(Regex(".+\n", RegexOption.DOT_MATCHES_ALL).matches("\n"))
        assertFalse(Regex(".+abc").containsMatchIn("abc"))
        assertFalse(Regex(".+\nabc", RegexOption.DOT_MATCHES_ALL).containsMatchIn("\nabc"))
        assertFalse(Regex(".+").containsMatchIn(""))
        assertFalse(Regex(".+\n", RegexOption.DOT_MATCHES_ALL).containsMatchIn("\n"))

        assertTrue(Regex(".+abc").matches("aabc"))
        assertTrue(Regex(".+\nabc", RegexOption.DOT_MATCHES_ALL).matches("a\nabc"))
        assertTrue(Regex(".+").matches("a"))
        assertTrue(Regex(".+\n", RegexOption.DOT_MATCHES_ALL).matches("a\n"))
        assertTrue(Regex(".+abc").containsMatchIn("aabc"))
        assertTrue(Regex(".+\nabc", RegexOption.DOT_MATCHES_ALL).containsMatchIn("a\nabc"))
        assertTrue(Regex(".+").containsMatchIn("a"))
        assertTrue(Regex(".+\n", RegexOption.DOT_MATCHES_ALL).containsMatchIn("a\n"))
    }

    @Test fun testQuantVsGroup() {
        val patternString = "(d{1,3})((a|c)*)(d{1,3})((a|c)*)(d{1,3})"
        val testString = "dacaacaacaaddaaacaacaaddd"

        val regex = Regex(patternString)

        val result = regex.matchEntire(testString)!!
        assertEquals("dacaacaacaaddaaacaacaaddd", result.groupValues[0])
        assertEquals("d", result.groupValues[1])
        assertEquals("acaacaacaa", result.groupValues[2])
        assertEquals("dd", result.groupValues[4])
        assertEquals("aaacaacaa", result.groupValues[5])
        assertEquals("ddd", result.groupValues[7])
    }

    /*
 * Class under test for boolean find()
 */
    @Test fun testFind() {
        var testPattern = "(abb)"
        var testString = "cccabbabbabbabbabb"
        val regex = Regex(testPattern)
        var result = regex.find(testString)
        var start = 3
        var end = 6
        while (result != null) {
            assertEquals(start, result.groups[1]!!.range.start)
            assertEquals(end, result.groups[1]!!.range.endInclusive + 1)

            start = end
            end += 3
            result = result.next()
        }

        testPattern = "(\\d{1,3})"
        testString = "aaaa123456789045"

        val regex2 = Regex(testPattern)
        var result2 = regex2.find(testString)
        start = 4
        val length = 3
        while (result2 != null) {
            assertEquals(testString.substring(start, start + length), result2.groupValues[1])
            start += length
            result2 = result2.next()
        }
    }

    @Test fun testSEOLsymbols() {
        val regex = Regex("^a\\(bb\\[$")
        assertTrue(regex.matches("a(bb["))
    }

    @Test fun testGroupCount() {
        for (i in groupPatterns.indices) {
            val regex = Regex(groupPatterns[i])
            val result = regex.matchEntire("ababababbaaabb")!!
            assertEquals(i + 1, result.groups.size - 1)
        }
    }

    @Test fun testReluctantQuantifiers() {
        val regex = Regex("(ab*)*b")
        val result = regex.matchEntire("abbbb")
        if (result != null) {
            assertEquals("abbb", result.groupValues[1])
        } else {
            fail("Match expected: (ab*)*b vs abbbb")
        }
    }

    @Test fun testEnhancedFind() {
        val input = "foob"
        val pattern = "a*b"
        val regex = Regex(pattern)
        val result = regex.find(input)!!
        assertEquals("b", result.groupValues[0])
    }

    @Test fun testPosCompositeGroup() {
        val posExamples = arrayOf("aabbcc", "aacc", "bbaabbcc")
        val negExamples = arrayOf("aabb", "bb", "bbaabb")
        val posPat = Regex("(aa|bb){1,3}+cc")
        val negPat = Regex("(aa|bb){1,3}+bb")

        for (element in posExamples) {
            assertTrue(posPat.matches(element))
        }

        for (element in negExamples) {
            assertFalse(negPat.matches(element))
        }

        assertTrue(Regex("(aa|bb){1,3}+bb").matches("aabbaabb"))
    }

    @Test fun testPosAltGroup() {
        val posExamples = arrayOf("aacc", "bbcc", "cc")
        val negExamples = arrayOf("bb", "aa")
        val posPat = Regex("(aa|bb)?+cc")
        val negPat = Regex("(aa|bb)?+bb")

        for (element in posExamples) {
            assertTrue(posPat.toString() + " vs: " + element, posPat.matches(element))
        }

        for (element in negExamples) {
            assertFalse(negPat.matches(element))
        }

        assertTrue(Regex("(aa|bb)?+bb").matches("aabb"))
    }

    @Test fun testRelCompGroup() {
        var res = ""
        for (i in 0..3) {
            val regex = Regex("((aa|bb){$i,3}?).*cc")
            val result = regex.matchEntire("aaaaaacc")
            assertTrue(regex.toString() + " vs: " + "aaaaaacc", result != null)
            assertEquals(res, result!!.groupValues[1])
            res += "aa"
        }
    }

    @Test fun testRelAltGroup() {
        var regex = Regex("((aa|bb)??).*cc")
        var result = regex.matchEntire("aacc")
        assertTrue(regex.toString() + " vs: " + "aacc", result != null)
        assertEquals("", result!!.groupValues[1])

        regex = Regex("((aa|bb)??)cc")
        result = regex.matchEntire("aacc")
        assertTrue(regex.toString() + " vs: " + "aacc", result != null)
        assertEquals("aa", result!!.groupValues[1])
    }

    @Test fun testIgnoreCase() {
        var regex = Regex("(aa|bb)*", RegexOption.IGNORE_CASE)
        assertTrue(regex.matches("aAbb"))

        regex = Regex("(a|b|c|d|e)*", RegexOption.IGNORE_CASE)
        assertTrue(regex.matches("aAebbAEaEdebbedEccEdebbedEaedaebEbdCCdbBDcdcdADa"))

        regex = Regex("[a-e]*", RegexOption.IGNORE_CASE)
        assertTrue(regex.matches("aAebbAEaEdebbedEccEdebbedEaedaebEbdCCdbBDcdcdADa"))
    }

    @Test fun testQuoteReplacement() {
        assertEquals("\\\\aaCC\\$1", Regex.escapeReplacement("\\aaCC$1"))
    }

    @Test fun testOverFlow() {
        var regex = Regex("(a*)*")
        var result = regex.matchEntire("aaa")
        assertTrue(result != null)
        assertEquals("", result!!.groupValues[1])

        assertTrue(Regex("(1+)\\1+").matches("11"))
        assertTrue(Regex("(1+)(2*)\\2+").matches("11"))

        regex = Regex("(1+)\\1*")
        result = regex.matchEntire("11")

        assertTrue(result != null)
        assertEquals("11", result!!.groupValues[1])

        regex = Regex("((1+)|(2+))(\\2+)")
        result = regex.matchEntire("11")

        assertTrue(result != null)
        assertEquals("1", result!!.groupValues[2])
        assertEquals("1", result.groupValues[1])
        assertEquals("1", result.groupValues[4])
        assertEquals("", result.groupValues[3])
    }

    @Test fun testUnicode() {
        assertTrue(Regex("\\x61a").matches("aa"))
        assertTrue(Regex("\\u0061a").matches("aa"))
        assertTrue(Regex("\\0141a").matches("aa"))
        assertTrue(Regex("\\0777").matches("?7"))
    }

    @Test fun testUnicodeCategory() {
        assertTrue(Regex("\\p{Ll}").matches("k")) // Unicode lower case
        assertTrue(Regex("\\P{Ll}").matches("K")) // Unicode non-lower
        // case
        assertTrue(Regex("\\p{Lu}").matches("K")) // Unicode upper case
        assertTrue(Regex("\\P{Lu}").matches("k")) // Unicode non-upper
        // case combinations
        assertTrue(Regex("[\\p{L}&&[^\\p{Lu}]]").matches("k"))
        assertTrue(Regex("[\\p{L}&&[^\\p{Ll}]]").matches("K"))
        assertFalse(Regex("[\\p{L}&&[^\\p{Lu}]]").matches("K"))
        assertFalse(Regex("[\\p{L}&&[^\\p{Ll}]]").matches("k"))

        // category/character combinations
        assertFalse(Regex("[\\p{L}&&[^a-z]]").matches("k"))
        assertTrue(Regex("[\\p{L}&&[^a-z]]").matches("K"))

        assertTrue(Regex("[\\p{Lu}a-z]").matches("k"))
        assertTrue(Regex("[a-z\\p{Lu}]").matches("k"))

        assertFalse(Regex("[\\p{Lu}a-d]").matches("k"))
        assertTrue(Regex("[a-d\\p{Lu}]").matches("K"))

        assertFalse(Regex("[\\p{L}&&[^\\p{Lu}&&[^G]]]").matches("K"))

    }

    @Test fun testSplitEmpty() {

        val regex = Regex("")
        val s = regex.split("", 0)

        assertEquals(2, s.size)
        assertEquals("", s[0])
        assertEquals("", s[1])
    }

    @Test fun testFindDollar() {
        val regex = Regex("a$")
        val result = regex.find("a\n")
        assertTrue(result != null)
        assertEquals("a", result!!.groupValues[0])
    }

    /*
     * Regression test for HARMONY-674
     */
    @Test fun testPatternMatcher() {
        assertTrue(Regex("(?:\\d+)(?:pt)").matches("14pt"))
    }

    /**
     * Inspired by HARMONY-3360
     */
    @Test fun test3360() {
        val str = "!\"#%&'(),-./"
        val regex = Regex("\\s")
        assertFalse(regex.containsMatchIn(str))
    }

    /**
     * Regression test for HARMONY-3360
     */
    @Test fun testGeneralPunctuationCategory() {
        val s = arrayOf(",", "!", "\"", "#", "%", "&", "'", "(", ")", "-", ".", "/")
        val regexp = "\\p{P}"

        for (i in s.indices) {
            val regex = Regex(regexp)
            assertTrue(regex.containsMatchIn(s[i]))
        }
    }

    /**
     * Regression test for https://github.com/JetBrains/kotlin-native/issues/2297
     */
    @Test fun test2297() {
        assertTrue(Regex("^(:[0-5]?[0-9])+$").matches(":20:30"))
        assertTrue(Regex("(.{1,}){2}").matches("aa"))

        assertTrue(Regex("(.+b)+").matches("0b0b"))
        assertTrue(Regex("(.+?b)+").matches("0b0b"))
        assertTrue(Regex("(.?b)+").matches("0b0b"))
        assertTrue(Regex("(.??b)+").matches("0b0b"))
        assertTrue(Regex("(.*b)+").matches("0b0b"))
        assertTrue(Regex("(.*?b)+").matches("0b0b"))
        assertTrue(Regex("(.{1,2}b)+").matches("0b00b"))
        assertTrue(Regex("(.{1,2}?b)+").matches("0b00b"))

        assertTrue(Regex("([0]?[0]?)+").matches("0000"))
        assertTrue(Regex("([0]?[0]?b)+").matches("00b00b"))
        assertTrue(Regex("((b{2}){3})+").matches("bbbbbbbbbbbb"))

        assertTrue(Regex("[^a]").matches("b"))
    }

    @Test fun kt28158() {
        val comment = "😃😃😃😃😃😃"
        val regex = Regex("(.{3,})\\1+", RegexOption.IGNORE_CASE)
        assertTrue(comment.contains(regex))
    }
}