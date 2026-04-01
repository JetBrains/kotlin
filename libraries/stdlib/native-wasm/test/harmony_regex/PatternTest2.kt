/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.text.harmony_regex

import kotlin.text.*
import kotlin.test.*

class PatternTest2 {

    fun assertTrue(msg: String, value: Boolean) = assertTrue(value, msg)
    fun assertFalse(msg: String, value: Boolean) = assertFalse(value, msg)

    private fun assertFindAll(regex: Regex, input: String, expected: List<String>) {
        assertEquals(expected, regex.findAll(input).map(MatchResult::value).toList(), "Unexpected matches for `$regex` in `$input`")
    }

    private fun assertFailsToCompile(pattern: String): IllegalArgumentException =
        assertFailsWith<IllegalArgumentException>("IllegalArgumentException expected for pattern `$pattern`") {
            Regex(pattern)
        }

    /**
     * Tests simple pattern compilation and matching methods
     */
    @Test fun testSimpleMatch() {
        val regex = Regex("foo.*")

        var testString = "foo123"
        assertTrue(regex.matches(testString))
        assertTrue(regex in testString)
        assertTrue(regex.find(testString) != null)

        testString = "fox"
        assertFalse(regex.matches(testString))
        assertFalse(regex in testString)
        assertFalse(regex.find(testString) != null)

        assertTrue(Regex("foo.*").matches("foo123"))
        assertFalse(Regex("foo.*").matches("fox"))
        assertFalse(Regex("bar").matches("foobar"))
        assertTrue(Regex("").matches(""))
    }

    @Test fun testCursors() {
        val regex: Regex
        var result: MatchResult?

        try {
            regex = Regex("foo")

            result = regex.find("foobar")
            assertNotNull(result)
            assertEquals(0, result!!.range.start)
            assertEquals(3, result.range.endInclusive + 1)
            assertNull(result.next())


            result = regex.find("barfoobar")
            assertNotNull(result)
            assertEquals(3, result!!.range.start)
            assertEquals(6, result.range.endInclusive + 1)
            assertNull(result.next())

            result = regex.find("barfoo")
            assertNotNull(result)
            assertEquals(3, result!!.range.start)
            assertEquals(6, result.range.endInclusive + 1)
            assertNull(result.next())

            result = regex.find("foobarfoobarfoo")
            assertNotNull(result)
            assertEquals(0, result!!.range.start)
            assertEquals(3, result.range.endInclusive + 1)
            result = result.next()
            assertNotNull(result)
            assertEquals(6, result!!.range.start)
            assertEquals(9, result.range.endInclusive + 1)
            result = result.next()
            assertNotNull(result)
            assertEquals(12, result!!.range.start)
            assertEquals(15, result.range.endInclusive + 1)
            assertNull(result.next())

            result = regex.find("foobarfoobarfoo", 0)
            assertNotNull(result)
            assertEquals(0, result!!.range.start)
            assertEquals(3, result.range.endInclusive + 1)

            result = regex.find("foobarfoobarfoo", 4)
            assertNotNull(result)
            assertEquals(6, result!!.range.start)
            assertEquals(9, result.range.endInclusive + 1)
        } catch (e: IllegalArgumentException) {
            println(e.message)
            fail()
        }

    }

    @Test fun testGroups() {
        val regex: Regex
        var result: MatchResult?

        regex = Regex("(p[0-9]*)#?(q[0-9]*)")

        result = regex.find("p1#q3p2q42p5p71p63#q888")
        assertNotNull(result)
        assertEquals(0, result!!.range.start)
        assertEquals(5, result.range.endInclusive + 1)
        assertEquals(3, result.groups.size)
        assertEquals(0, result.groups[0]!!.range.start)
        assertEquals(5, result.groups[0]!!.range.endInclusive + 1)
        assertEquals(0, result.groups[1]!!.range.start)
        assertEquals(2, result.groups[1]!!.range.endInclusive + 1)
        assertEquals(3, result.groups[2]!!.range.start)
        assertEquals(5, result.groups[2]!!.range.endInclusive + 1)
        assertEquals("p1#q3", result.value)
        assertEquals("p1#q3", result.groupValues[0])
        assertEquals("p1", result.groupValues[1])
        assertEquals("q3", result.groupValues[2])

        result = result.next()
        assertNotNull(result)
        assertEquals(5, result!!.range.start)
        assertEquals(10, result.range.endInclusive + 1)
        assertEquals(3, result.groups.size)
        assertEquals(10, result.groups[0]!!.range.endInclusive + 1)
        assertEquals(5, result.groups[1]!!.range.start)
        assertEquals(7, result.groups[1]!!.range.endInclusive + 1)
        assertEquals(7, result.groups[2]!!.range.start)
        assertEquals(10, result.groups[2]!!.range.endInclusive + 1)
        assertEquals("p2q42", result.value)
        assertEquals("p2q42", result.groupValues[0])
        assertEquals("p2", result.groupValues[1])
        assertEquals("q42", result.groupValues[2])

        result = result.next()
        assertNotNull(result)
        assertEquals(15, result!!.range.start)
        assertEquals(23, result.range.endInclusive + 1)
        assertEquals(3, result.groups.size)
        assertEquals(15, result.groups[0]!!.range.start)
        assertEquals(23, result.groups[0]!!.range.endInclusive + 1)
        assertEquals(15, result.groups[1]!!.range.start)
        assertEquals(18, result.groups[1]!!.range.endInclusive + 1)
        assertEquals(19, result.groups[2]!!.range.start)
        assertEquals(23, result.groups[2]!!.range.endInclusive + 1)
        assertEquals("p63#q888", result.value)
        assertEquals("p63#q888", result.groupValues[0])
        assertEquals("p63", result.groupValues[1])
        assertEquals("q888", result.groupValues[2])
        assertNull(result.next())
    }

    @Test fun testReplace() {
        var regex: Regex

        // Note: examples from book,
        // Hitchens, Ron, 2002, "Java NIO", O'Reilly, page 171
        regex = Regex("a*b")

        var testString = "aabfooaabfooabfoob"
        assertTrue(regex.replace(testString, "-") == "-foo-foo-foo-")
        assertTrue(regex.replaceFirst(testString, "-") == "-fooaabfooabfoob")

        regex = Regex("([bB])yte")

        testString = "Byte for byte"
        assertTrue(regex.replaceFirst(testString, "$1ite") == "Bite for byte")
        assertTrue(regex.replace(testString, "$1ite") == "Bite for bite")

        regex = Regex("\\d\\d\\d\\d([- ])")

        testString = "card #1234-5678-1234"
        assertTrue(regex.replaceFirst(testString, "xxxx$1") == "card #xxxx-5678-1234")
        assertTrue(regex.replace(testString, "xxxx$1") == "card #xxxx-xxxx-1234")

        regex = Regex("(up|left)( *)(right|down)")

        testString = "left right, up down"
        assertTrue(regex.replaceFirst(testString, "$3$2$1") == "right left, up down")
        assertTrue(regex.replace(testString, "$3$2$1") == "right left, down up")

        regex = Regex("([CcPp][hl]e[ea]se)")

        testString = "I want cheese. Please."
        assertTrue(regex.replaceFirst(testString, "<b> $1 </b>") == "I want <b> cheese </b>. Please.")
        assertTrue(regex.replace(testString, "<b> $1 </b>") == "I want <b> cheese </b>. <b> Please </b>.")
    }

    @Test fun testEscapes() {
        var regex: Regex
        var result: MatchResult?

        // Test \\ sequence
        regex = Regex("([a-z]+)\\\\([a-z]+);")
        result = regex.find("fred\\ginger;abbott\\costello;jekell\\hyde;")
        assertNotNull(result)
        assertEquals("fred", result!!.groupValues[1])
        assertEquals("ginger", result.groupValues[2])
        result = result.next()
        assertNotNull(result)
        assertEquals("abbott", result!!.groupValues[1])
        assertEquals("costello", result.groupValues[2])
        result = result.next()
        assertNotNull(result)
        assertEquals("jekell", result!!.groupValues[1])
        assertEquals("hyde", result.groupValues[2])
        assertNull(result.next())

        // Test \n, \t, \r, \f, \e, \a sequences
        regex = Regex("([a-z]+)[\\n\\t\\r\\f\\e\\a]+([a-z]+)")
        result = regex.find("aa\nbb;cc\u0009\rdd;ee\u000C\u001Bff;gg\n\u0007hh")
        assertNotNull(result)
        assertEquals("aa", result!!.groupValues[1])
        assertEquals("bb", result.groupValues[2])
        result = result.next()
        assertNotNull(result)
        assertEquals("cc", result!!.groupValues[1])
        assertEquals("dd", result.groupValues[2])
        result = result.next()
        assertNotNull(result)
        assertEquals("ee", result!!.groupValues[1])
        assertEquals("ff", result.groupValues[2])
        result = result.next()
        assertNotNull(result)
        assertEquals("gg", result!!.groupValues[1])
        assertEquals("hh", result.groupValues[2])
        assertNull(result.next())

        // Test \\u and \\x sequences
        regex = Regex("([0-9]+)[\\u0020:\\x21];")
        result = regex.find("11:;22 ;33-;44!;")
        assertNotNull(result)
        assertEquals("11", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("22", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("44", result!!.groupValues[1])
        assertNull(result.next())

        // Test invalid unicode sequences // TODO: Double check it.
        try {
            regex = Regex("\\u")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\u;")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\u002")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\u002;")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        // Test invalid hex sequences
        try {
            regex = Regex("\\x")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\x;")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\xa")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\xa;")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        // Test \0 (octal) sequences (1, 2 and 3 digit)
        regex = Regex("([0-9]+)[\\07\\040\\0160];")
        result = regex.find("11\u0007;22:;33 ;44p;")
        assertNotNull(result)
        assertEquals("11", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("33", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("44", result!!.groupValues[1])
        assertNull(result.next())

        // Test invalid octal sequences
        try {
            regex = Regex("\\08")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\0")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\0;")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        // Test \c (control character) sequence
        regex = Regex("([0-9]+)[\\cA\\cB\\cC\\cD];")
        result = regex.find("11\u0001;22:;33\u0002;44p;55\u0003;66\u0004;")
        assertNotNull(result)
        assertEquals("11", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("33", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("55", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("66", result!!.groupValues[1])
        assertNull(result.next())

        // More thorough control escape test
        // Ensure that each escape matches exactly the corresponding
        // character
        // code and no others (well, from 0-255 at least)
        for (i in 0..25) {
            regex = Regex("\\c${'A' + i}")
            var match_char = -1
            for (j in 0..255) {
                if (regex.matches("${j.toChar()}")) {
                    assertEquals(-1, match_char)
                    match_char = j
                }
            }
            assertTrue(match_char == i + 1)
        }


        // Test invalid control escapes
        try {
            regex = Regex("\\c")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test fun testCharacterClasses() {
        var regex: Regex

        // Test one character range
        regex = Regex("[p].*[l]")
        assertTrue(regex.matches("paul"))
        assertTrue(regex.matches("pool"))
        assertFalse(regex.matches("pong"))
        assertTrue(regex.matches("pl"))

        // Test two character range
        regex = Regex("[pm].*[lp]")
        assertTrue(regex.matches("prop"))
        assertTrue(regex.matches("mall"))
        assertFalse(regex.matches("pong"))
        assertTrue(regex.matches("pill"))

        // Test range including [ and ]
        regex = Regex("[<\\[].*[\\]>]")
        assertTrue(regex.matches("<foo>"))
        assertTrue(regex.matches("[bar]"))
        assertFalse(regex.matches("{foobar]"))
        assertTrue(regex.matches("<pill]"))

        // Test range using ^
        regex = Regex("[^bc][a-z]+[tr]")
        assertTrue(regex.matches("pat"))
        assertTrue(regex.matches("liar"))
        assertFalse(regex.matches("car"))
        assertTrue(regex.matches("gnat"))

        // Test character range using -
        regex = Regex("[a-z]_+[a-zA-Z]-+[0-9p-z]")
        assertTrue(regex.matches("d__F-8"))
        assertTrue(regex.matches("c_a-q"))
        assertFalse(regex.matches("a__R-a"))
        assertTrue(regex.matches("r_____d-----5"))

        // Test range using unicode characters and unicode and hex escapes
        regex = Regex("[\\u1234-\\u2345]_+[a-z]-+[\u0001-\\x11]")
        assertTrue(regex.matches("\u2000_q-\u0007"))
        assertTrue(regex.matches("\u1234_z-\u0001"))
        assertFalse(regex.matches("r_p-q"))
        assertTrue(regex.matches("\u2345_____d-----\n"))

        // Test ranges including the "-" character
        regex = Regex("[\\*-/]_+[---]!+[--AP]")
        assertTrue(regex.matches("-_-!!A"))
        assertTrue(regex.matches("\u002b_-!!!-"))
        assertFalse(regex.matches("!_-!@"))
        assertTrue(regex.matches(",______-!!!!!!!P"))

        // Test nested ranges
        regex = Regex("[pm[t]][a-z]+[[r]lp]")
        assertTrue(regex.matches("prop"))
        assertTrue(regex.matches("tsar"))
        assertFalse(regex.matches("pong"))
        assertTrue(regex.matches("moor"))

        // Test character class intersection with &&
        // TODO: figure out what x&&y or any class with a null intersection
        // set (like [[a-c]&&[d-f]]) might mean. It doesn't mean "match
        // nothing" and doesn't mean "match anything" so I'm stumped.
        regex = Regex("[[a-p]&&[g-z]]+-+[[a-z]&&q]-+[x&&[a-z]]-+")
        assertTrue(regex.matches("h--q--x--"))
        assertTrue(regex.matches("hog--q-x-"))
        assertFalse(regex.matches("ape--q-x-"))
        assertTrue(regex.matches("mop--q-x----"))

        // Test error cases with &&
        // TODO: What is this check?
        regex = Regex("[&&[xyz]]")
        regex.matches("&")
        regex.matches("x")
        regex.matches("y")
        regex = Regex("[[xyz]&[axy]]")
        regex.matches("x")
        regex.matches("z")
        regex.matches("&")
        regex = Regex("[abc[123]&&[345]def]")
        regex.matches("a")

        regex = Regex("[[xyz]&&]")
        regex = Regex("[[abc]&]")

        try {
            regex = Regex("[[abc]&&")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        regex = Regex("[[abc]\\&&[xyz]]")
        regex = Regex("[[abc]&\\&[xyz]]")

        // Test 3-way intersection
        regex = Regex("[[a-p]&&[g-z]&&[d-k]]")
        assertTrue(regex.matches("g"))
        assertFalse(regex.matches("m"))

        // Test nested intersection
        regex = Regex("[[[a-p]&&[g-z]]&&[d-k]]")
        assertTrue(regex.matches("g"))
        assertFalse(regex.matches("m"))

        // Test character class subtraction with && and ^
        regex = Regex("[[a-z]&&[^aeiou]][aeiou][[^xyz]&&[a-z]]")
        assertTrue(regex.matches("pop"))
        assertTrue(regex.matches("tag"))
        assertFalse(regex.matches("eat"))
        assertFalse(regex.matches("tax"))
        assertTrue(regex.matches("zip"))

        // Test . (DOT), with and without DOTALL
        // Note: DOT not allowed in character classes
        regex = Regex(".+/x.z")
        assertTrue(regex.matches("!$/xyz"))
        assertFalse(regex.matches("%\n\r/x\nz"))
        regex = Regex(".+/x.z", RegexOption.DOT_MATCHES_ALL)
        assertTrue(regex.matches("%\n\r/x\nz"))

        // Test \d (digit)
        regex = Regex("\\d+[a-z][\\dx]")
        assertTrue(regex.matches("42a6"))
        assertTrue(regex.matches("21zx"))
        assertFalse(regex.matches("ab6"))
        assertTrue(regex.matches("56912f9"))

        // Test \D (not a digit)
        regex = Regex("\\D+[a-z]-[\\D3]")
        assertTrue(regex.matches("za-p"))
        assertTrue(regex.matches("%!e-3"))
        assertFalse(regex.matches("9a-x"))
        assertTrue(regex.matches("\u1234pp\ny-3"))

        // Test \s (whitespace)
        regex = Regex("<[a-zA-Z]+\\s+[0-9]+[\\sx][^\\s]>")
        assertTrue(regex.matches("<cat \t1 x>"))
        assertFalse(regex.matches("<cat \t1  >"))
        val result = regex.find("xyz <foo\n\r22 5> <pp \t\n \r \u000b41x\u1234><pp \nx7\rc> zzz")
        assertNotNull(result)
        assertNotNull(result!!.next())
        assertNull(result.next()!!.next())

        // Test \S (not whitespace) // TODO: We've removed \f from string since kotlin doesn't recognize this escape in a string.
        regex = Regex("<[a-z] \\S[0-9][\\S\n]+[^\\S]221>")
        assertTrue(regex.matches("<f $0**\n** 221>"))
        assertTrue(regex.matches("<x 441\t221>"))
        assertFalse(regex.matches("<z \t9\ng 221>"))
        assertTrue(regex.matches("<z 60\ngg\u1234 221>"))
        regex = Regex("<[a-z] \\S[0-9][\\S\n]+[^\\S]221[\\S&&[^abc]]>")
        assertTrue(regex.matches("<f $0**\n** 221x>"))
        assertTrue(regex.matches("<x 441\t221z>"))
        assertFalse(regex.matches("<x 441\t221 >"))
        assertFalse(regex.matches("<x 441\t221c>"))
        assertFalse(regex.matches("<z \t9\ng 221x>"))
        assertTrue(regex.matches("<z 60\ngg\u1234 221\u0001>"))

        // Test \w (ascii word)
        regex = Regex("<\\w+\\s[0-9]+;[^\\w]\\w+/[\\w$]+;")
        assertTrue(regex.matches("<f1 99;!foo5/a$7;"))
        assertFalse(regex.matches("<f$ 99;!foo5/a$7;"))
        assertTrue(regex.matches("<abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789 99;!foo5/a$7;"))

        // Test \W (not an ascii word)
        regex = Regex("<\\W\\w+\\s[0-9]+;[\\W_][^\\W]+\\s[0-9]+;")
        assertTrue(regex.matches("<\$foo3\n99;_bar\t0;"))
        assertFalse(regex.matches("<hh 99;_g 0;"))
        assertTrue(regex.matches("<*xx\t00;^zz 11;"))

        // Test x|y pattern
        regex = Regex("(?:[a-z]|[0-9])+")
        assertTrue(regex.matches("a"))
        assertTrue(regex.matches("0"))
        assertTrue(regex.matches("a24"))
        assertFalse(regex.matches("A"))
        assertFalse(regex.matches("A24"))
    }

    @Test fun testPOSIXGroups() {
        var regex: Regex

        // Test POSIX groups using \p and \P (in the group and not in the group)
        // Groups are Lower, Upper, ASCII, Alpha, Digit, XDigit, Alnum, Punct,
        // Graph, Print, Blank, Space, Cntrl
        // Test \p{Lower}
        /*
         * FIXME: Requires complex range processing p = Regex("<\\p{Lower}\\d\\P{Lower}:[\\p{Lower}Z]\\s[^\\P{Lower}]>");
         * m = p.matcher("<a4P:g x>"); assertTrue(m.matches()); m = p.matcher("<p4%:Z\tq>");
         * assertTrue(m.matches()); m = p.matcher("<A6#:e e>");
         * assertFalse(m.matches());
         */
        regex = Regex("\\p{Lower}+")
        assertTrue(regex.matches("abcdefghijklmnopqrstuvwxyz"))

        // Invalid uses of \p{Lower}
        try {
            regex = Regex("\\p")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\p;")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\p{")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\p{;")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\p{Lower")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\p{Lower;")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        // Test \p{Upper}
        /*
         * FIXME: Requires complex range processing p = Regex("<\\p{Upper}\\d\\P{Upper}:[\\p{Upper}z]\\s[^\\P{Upper}]>");
         * m = p.matcher("<A4p:G X>"); assertTrue(m.matches()); m = p.matcher("<P4%:z\tQ>");
         * assertTrue(m.matches()); m = p.matcher("<a6#:E E>");
         * assertFalse(m.matches());
         */
        regex = Regex("\\p{Upper}+")
        assertTrue(regex.matches("ABCDEFGHIJKLMNOPQRSTUVWXYZ"))

        // Invalid uses of \p{Upper}
        try {
            regex = Regex("\\p{Upper")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\p{Upper;")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        // Test \p{ASCII}
        /*
         * FIXME: Requires complex range processing p = Regex("<\\p{ASCII}\\d\\P{ASCII}:[\\p{ASCII}\u1234]\\s[^\\P{ASCII}]>");
         * m = p.matcher("<A4\u0080:G X>"); assertTrue(m.matches()); m =
         * p.matcher("<P4\u00ff:\u1234\t\n>"); assertTrue(m.matches()); m =
         * p.matcher("<\u00846#:E E>"); assertFalse(m.matches())
         */
        regex = Regex("\\p{ASCII}")
        for (i in 0 until 0x80) {
            assertTrue(regex.matches("${i.toChar()}"))
        }
        for (i in 0x80..0xff) {
            assertFalse(regex.matches("${i.toChar()}"))
        }

        // Invalid uses of \p{ASCII}
        try {
            regex = Regex("\\p{ASCII")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        try {
            regex = Regex("\\p{ASCII;")
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
        }

        // Test \p{Alpha}
        regex = Regex("\\p{Alpha}+")
        assertTrue(regex.matches("abcXYZ"), "\\p{Alpha}+ should match ASCII letters")
        assertFalse(regex.matches("abc123"))
        assertFalse(regex.matches("\u00c0\u00df\u03a9"), "\\p{Alpha}+ should not match non-ASCII letters")

        // Test \p{Digit}
        regex = Regex("\\p{Digit}+")
        assertTrue(regex.matches("0123456789"), "\\p{Digit}+ should match ASCII digits")
        assertFalse(regex.matches("12a45"))
        assertFalse(regex.matches("١٢٣"), "\\p{Digit}+ should not match non-ASCII digits")

        // Test \p{XDigit}
        regex = Regex("\\p{XDigit}+")
        assertTrue(regex.matches("0123456789abcdefABCDEF"), "\\p{XDigit}+ should match hexadecimal digits")
        assertFalse(regex.matches("01234xyz"))
        assertFalse(regex.matches("١٢٣"), "\\p{XDigit}+ should not match non-ASCII digits")

        // Test \p{Alnum}
        regex = Regex("\\p{Alnum}+")
        assertTrue(regex.matches("abcXYZ123"), "\\p{Alnum}+ should match ASCII letters and digits")
        assertFalse(regex.matches("abc-123"))
        assertFalse(regex.matches("\u00c0١٢٣"), "\\p{Alnum}+ should not match non-ASCII letters or digits")

        // Test \p{Punct}
        regex = Regex("\\p{Punct}+")
        assertTrue(regex.matches("!\"#\$%&'()*+,-./:;<=>?@[\\\\]^_`{|}~"), "\\p{Punct}+ should match ASCII punctuation")
        assertFalse(regex.matches("abc!"))
        assertFalse(regex.matches("«»"), "\\p{Punct}+ should not match non-ASCII punctuation")

        // Test \p{Graph}
        regex = Regex("\\p{Graph}+")
        assertTrue(regex.matches("abcXYZ123!@#"), "\\p{Graph}+ should match visible ASCII characters")
        assertFalse(regex.matches("abc xyz"))
        assertFalse(regex.matches("abc\u00a1"), "\\p{Graph}+ should not match non-ASCII graphic characters")

        // Test \p{Print}
        regex = Regex("\\p{Print}+")
        // TODO: Ignored: KT-85324
        // assertTrue(regex.matches(" "), "\\p{Print}+ should match a space")
        // assertTrue(regex.matches("!"), "\\p{Print}+ should match printable punctuation")
        // assertFalse(regex.matches("\u00a0"), "\\p{Print}+ should not match non-ASCII printable characters")

        // Test \p{Blank}
        regex = Regex("\\p{Blank}+")
        assertTrue(regex.matches(" \t  \t"), "\\p{Blank}+ should match spaces and tabs")
        assertFalse(regex.matches(" \n"))
        assertFalse(regex.matches("\u00a0"), "\\p{Blank}+ should not match non-ASCII space separators")

        // Test \p{Space}
        regex = Regex("\\p{Space}+")
        assertTrue(regex.matches(" \t\n\r\u000B\u000C"), "\\p{Space}+ should match ASCII whitespace")
        assertFalse(regex.matches(" a "))
        assertFalse(regex.matches("\u00a0"), "\\p{Space}+ should not match non-ASCII space separators")

        // Test \p{Cntrl}
        regex = Regex("\\p{Cntrl}+")
        assertTrue(regex.matches("\u0000\u0001\u001F\u007F"), "\\p{Cntrl}+ should match ASCII control characters")
        assertFalse(regex.matches("\u0000A"))
        assertFalse(regex.matches("\u0085"), "\\p{Cntrl}+ should not match non-ASCII control characters")
    }

    // Regression test for KT-80603
    @Test fun testNumberCategory() {
        val regex = Regex("\\p{IsN}+")
        assertEquals(listOf("٧٨٩", "०", "๙", "Ⅴ", "Ⅹ", "ⅻ", "123", "½", "¼", "¾", "²", "³", "456"),
            regex.findAll("٧٨٩ ० ๙ Ⅴ Ⅹ ⅻ 123 ½ ¼ ¾ x² + y³ 中文 العربية456")
                .map(MatchResult::value)
                .toList()
        )
    }

    @Test fun testUnicodeBlocks() {
        var regex: Regex

        // Test Unicode blocks using \p and \P
        for (block in UBlocks) {
            regex = Regex("\\p{In" + block.name + "}")
            if (block.low > 0) {
                assertFalse(regex.matches((block.low - 1).toChar().toString()))
            }
            for (i in block.low..block.high) {
                assertTrue(regex.matches(i.toChar().toString()))
            }
            if (block.high < 0xFFFF) {
                assertFalse(regex.matches((block.high + 1).toChar().toString()))
            }

            regex = Regex("\\P{In" + block.name + "}")
            if (block.low > 0) {
                assertTrue(regex.matches((block.low - 1).toChar().toString()))
            }
            for (i in block.low..block.high) {
                assertFalse("assert: Regex: $regex, match to: ${i.toChar()} ($i)", regex.matches(i.toChar().toString()))
            }
            if (block.high < 0xFFFF) {
                assertTrue(regex.matches((block.high + 1).toChar().toString()))
            }

        }
    }

    @Test fun testCapturingGroups() {
        // Test simple capturing groups
        var result = Regex("(ab)(cd)").matchEntire("abcd")
        assertNotNull(result)
        assertEquals(listOf("abcd", "ab", "cd"), result.groupValues)

        // Test grouping without capture (?:...)
        result = Regex("(?:ab)+(cd)").matchEntire("ababcd")
        assertNotNull(result)
        assertEquals(2, result.groups.size)
        assertEquals("cd", result.groupValues[1])

        // Test combination of grouping and capture
        result = Regex("((?:ab)+)-(c(d))").matchEntire("abab-cd")
        assertNotNull(result)
        assertEquals(listOf("abab-cd", "abab", "cd", "d"), result.groupValues)

        // Test \<num> sequence with capturing and non-capturing groups
        val regex = Regex("((?:ab)+)-(?:cd)-\\1")
        assertTrue(regex.matches("abab-cd-abab"))
        assertFalse(regex.matches("abab-cd-ab"))

        // Test \<num> with <num> out of range
        assertFailsToCompile("(a)\\2")
    }

    @Test fun testRepeats() {
        // Test ?
        var regex = Regex("colou?r")
        assertTrue(regex.matches("color"))
        assertTrue(regex.matches("colour"))
        assertFalse(regex.matches("colr"))
        assertFalse(regex.matches("colouur"))
        assertFalse(regex.matches("xcolor"))

        // Test *
        regex = Regex("ab*c")
        assertTrue(regex.matches("ac"))
        assertTrue(regex.matches("abbbc"))
        assertFalse(regex.matches("ab"))
        assertFalse(regex.matches("abdc"))
        assertFalse(regex.matches("cabbbc"))

        // Test +
        regex = Regex("ab+c")
        assertFalse(regex.matches("ac"))
        assertTrue(regex.matches("abc"))
        assertTrue(regex.matches("abbbbbc"))
        assertFalse(regex.matches("abb"))
        assertFalse(regex.matches("xabc"))

        // Test {<num>}, including 0, 1 and more
        assertTrue(Regex("a{0}b").matches("b"))
        assertTrue(Regex("a{1}b").matches("ab"))
        assertTrue(Regex("a{3}b").matches("aaab"))
        assertFalse(Regex("a{1}b").matches("b"))
        assertFalse(Regex("a{3}b").matches("aaaab"))
        assertFalse(Regex("a{3}b").matches("aab"))

        // Test {<num>,}, including 0, 1 and more
        assertTrue(Regex("a{0,}b").matches("b"))
        assertTrue(Regex("a{1,}b").matches("ab"))
        assertTrue(Regex("a{3,}b").matches("aaaaab"))
        assertFalse(Regex("a{1,}b").matches("b"))
        assertFalse(Regex("a{3,}b").matches("aab"))
        assertFalse(Regex("a{3,}b").matches("aaaaac"))

        // Test {<n1>,<n2>}, with n1 < n2, n1 = n2 and n1 > n2 (illegal?)
        assertTrue(Regex("a{2,4}b").matches("aaab"))
        assertFalse(Regex("a{2,4}b").matches("ab"))
        assertFalse(Regex("a{2,4}b").matches("aaaaab"))
        assertTrue(Regex("a{3,3}b").matches("aaab"))
        assertFalse(Regex("a{3,3}b").matches("aab"))
        assertFalse(Regex("a{3,3}b").matches("aaaab"))
        assertFailsToCompile("a{4,2}b")
    }

    @Test fun testAnchors() {
        // Test ^, default and MULTILINE
        assertTrue(Regex("^foo").containsMatchIn("foo\nbar"))
        assertFalse(Regex("^bar").containsMatchIn("foo\nbar"))
        assertTrue(Regex("^foo", RegexOption.MULTILINE).containsMatchIn("foo\nbar"))
        assertTrue(Regex("^bar", RegexOption.MULTILINE).containsMatchIn("foo\nbar"))

        // Test $, default and MULTILINE
        assertTrue(Regex("bar$").containsMatchIn("foo\nbar"))
        assertFalse(Regex("foo$").containsMatchIn("foo\nbar"))
        assertTrue(Regex("foo$", RegexOption.MULTILINE).containsMatchIn("foo\nbar"))
        assertTrue(Regex("bar$", RegexOption.MULTILINE).containsMatchIn("foo\nbar"))

        // Test \b (word boundary)
        assertTrue(Regex("\\bcat\\b").containsMatchIn("a cat!"))
        assertFalse(Regex("\\bcat\\b").containsMatchIn("concatenate"))

        // Test \B (not a word boundary)
        assertTrue(Regex("c\\Bat").containsMatchIn("category"))
        assertTrue(Regex("c\\Bat").containsMatchIn("cat"))
        assertFalse(Regex("c\\Bat").containsMatchIn("c-at"))
        assertTrue(Regex("\\B").matches(""))

        // Test \A (beginning of string)
        assertTrue(Regex("\\Afoo").containsMatchIn("foo\nbar"))
        assertFalse(Regex("\\Abar").containsMatchIn("foo\nbar"))
        assertFalse(Regex("\\Abar", RegexOption.MULTILINE).containsMatchIn("foo\nbar"))
        assertTrue(Regex("\\A").matches(""))

        // Test \Z (end of string)
        assertFalse(Regex("foo\\Z").matches("foo\n"))
        assertFalse(Regex("foo\\Z", RegexOption.MULTILINE).matches("foo\n"))
        assertTrue(Regex("foo\\Z").matches("foo"))
        assertTrue(Regex("\\Z").matches(""))

        // Test \z (end of string)
        assertFalse(Regex("foo\\z").matches("foo\n"))
        assertTrue(Regex("foo\\z").matches("foo"))
        assertTrue(Regex("\\z").matches(""))

        // Test \G
        assertFindAll(Regex("\\G[0-9]{2}"), "123456 78", listOf("12", "34", "56"))

        // Test positive lookahead using (?=...)
        assertFindAll(Regex("\\w+(?==)"), "LC_ALL=C LANG", listOf("LC_ALL"))
        assertFalse(Regex("\\w+(?==)").containsMatchIn("LC_ALL"))

        // Test negative lookahead using (?!...)
        assertFindAll(Regex("\\w+(?!=)"), "LC_ALL=C LANG", listOf("LC_AL", "C", "LANG"))
        assertFalse(Regex("\\w+(?!=)").containsMatchIn("=("))

        // Test positive lookbehind using (?<=...)
        assertFindAll(Regex("(?<=#)\\w+"), "a #tag and #topic", listOf("tag", "topic"))
        assertFalse(Regex("(?<=#)\\w+").containsMatchIn("not sharp enough"))

        // Test negative lookbehind using (?<!...)
        assertFindAll(Regex("(?<!#)\\b\\w+\\b"), "a #tag and plain", listOf("a", "and", "plain"))
        assertFalse(Regex("(?<!#)\\b\\w+\\b").containsMatchIn("#too #sharp"))
    }

    fun testRegexMatching(
        pattern: String,
        matchingStrings: List<String>,
        nonMatchingStrings: List<String> = emptyList(),
        vararg options: RegexOption
    ) {
        val regex = Regex(pattern, options.toSet())
        matchingStrings.forEach {
            assertTrue(regex.matches(it), "$regex did not match '$it'")
        }
        nonMatchingStrings.forEach {
            assertFalse(regex.matches(it), "$regex should not match '$it'")
        }
    }

    @Test fun testInlineIgnoreCaseFlag() {
        testRegexMatching("(?i)abc", listOf("abc", "ABC", "aBc"))
        testRegexMatching("a(?i)bc", listOf("aBC", "abc", "aBc"), listOf("Abc"))
        testRegexMatching("(?i)(?-i)a", listOf("a"), listOf("A"))
        testRegexMatching("(?-i)(?i)a", listOf("a", "A"))
        testRegexMatching("a(?i)b(?-i)c", listOf("abc", "aBc"), listOf("Abc", "AbC", "abC"))
        testRegexMatching("(?-i)abc", listOf("abc"), listOf("ABC"), RegexOption.IGNORE_CASE)
        testRegexMatching("ab(?-i)c", listOf("abc", "ABc", "aBc"), listOf("ABC"), RegexOption.IGNORE_CASE)
        testRegexMatching("a(?i:bc)d", listOf("aBCd", "abCd"), listOf("AbcD", "Abcd", "abcD"))
        testRegexMatching("(?i)a(?-i:bc)d", listOf("AbcD", "abcD", "Abcd"), listOf("aBCd", "abCd", "aBcd"))
        testRegexMatching("a(?-i:b)c", listOf("Abc", "abC"), listOf("aBc"), RegexOption.IGNORE_CASE)
        testRegexMatching("(?i:(?-i:(?i:abc)))", listOf("aBc"))
    }

    @Test fun testInlineDotAllFlag() {
        testRegexMatching("(?s)a.c", listOf("abc", "a\nc"))
        testRegexMatching(".(?s).c", listOf("abc", "a\nc"), listOf("\nbc"))
        testRegexMatching("(?s)(?-s).", listOf("a"), listOf("\n"))
        testRegexMatching("(?-s)(?s).", listOf("a", "\n"))
        testRegexMatching(".(?s).(?-s).", listOf("abc", "a\nc"), listOf("\n\n\n", "\nab", "ab\n"))
        testRegexMatching("(?-s).", emptyList(), listOf("\n"), RegexOption.DOT_MATCHES_ALL)
        testRegexMatching(".(?-s).", listOf("ab", "\na"), listOf("\n\n", "a\n"), RegexOption.DOT_MATCHES_ALL)
        testRegexMatching(".(?s:.).", listOf("abc", "a\nb"), listOf("\n\n\n", "\nab", "ab\n"))
        testRegexMatching("(?s).(?-s:.).", listOf("abc", "\n_\n", "\nab", "ab\n"), listOf("\n\n\n", "a\nb"))
        testRegexMatching(".(?-s:.).", listOf("abc", "\n_\n", "\nab", "ab\n"), listOf("\n\n\n", "a\nb"), RegexOption.DOT_MATCHES_ALL)
        testRegexMatching("(?s:(?-s:(?s:.)))", listOf("\n"))
    }

    @Test fun testInlineCommentsFlag() {
        // TODO: Ignored: KT-85425
        // testRegexMatching("(?x)#a b c\nd", listOf("d"), listOf("b c\nd"))
        // testRegexMatching("(?x)(?-x)#a b c", listOf("#a b c"))
        // testRegexMatching("(?-x:#a b c)", listOf("#a b c"), emptyList(), RegexOption.COMMENTS)
        testRegexMatching("(?x)a b", listOf("ab"), listOf("a b"))
        testRegexMatching("(?x)a b #comment\nc", listOf("abc"))
        testRegexMatching("(?x)a b(?-x)c d", listOf("abc d"), listOf("a bc d", "abcd"))
        testRegexMatching("(?-x)a b c", listOf("a b c"), listOf("abc"), RegexOption.COMMENTS)
        testRegexMatching(" (?x:a b) ", listOf(" ab "), listOf(" a b "))
        // TODO: Ignored: KT-85425
        //testRegexMatching(" (?-x:a b) ", listOf("a b"), listOf("ab", " a b "), RegexOption.COMMENTS)
        testRegexMatching("(?x:(?-x:(?x:a b c)))", listOf("abc"))
    }

    @Test fun testInlineMultilineFlag() {
        Regex("(?m)^[a-z]$").let { regex ->
            assertFindAll(regex, "a", listOf("a"))
            assertFindAll(regex, "a\nb", listOf("a", "b"))
            assertFindAll(regex, "a\nb\r\nc\rd", listOf("a", "b", "c", "d"))
        }
        Regex("(?m)(?-m)^[a-z]$").let { regex ->
            assertTrue(regex.matches("a"))
            assertFalse(regex.containsMatchIn("a\nb"))
        }
        Regex("(?-m)^[a-z]$", RegexOption.MULTILINE).let { regex ->
            assertTrue(regex.matches("a"))
            assertFalse(regex.containsMatchIn("a\nb"))
        }
        Regex("(?m)^a$\n(?-m)^b$").let { regex ->
            assertFalse(regex.matches("a\nb"))
        }
        Regex("a\n(?m:^[a-z]+$)\nb").let { regex ->
            assertTrue(regex.matches("a\nline\nb"))
        }
        Regex("(?m:(?-m:(?m:^[a-z]$)))").let { regex ->
            assertFindAll(regex, "a\r\nb\nc", listOf("a", "b", "c"))
        }
    }

    @Test fun testInlineUnixLinesFlag() {
        testRegexMatching("(?d).", listOf("\r"), listOf("\n"))
        testRegexMatching("(?-d).", emptyList(), listOf("\r", "\n"), RegexOption.UNIX_LINES)
        testRegexMatching("(?s-d).", listOf("\n", "\r"))
        testRegexMatching("(?-d).", listOf("\n", "\r"), emptyList(), RegexOption.DOT_MATCHES_ALL)
        testRegexMatching("(?d)(?-d).", listOf("_"), listOf("\n", "\r"))

        testRegexMatching(".(?d)..", listOf("_\r\r"), listOf("\r\r\r", "_\n\r"))
        testRegexMatching(".(?-d)..", listOf("\r__"), listOf("\r\r\r"), RegexOption.UNIX_LINES)
        testRegexMatching("(?d:(?-d:(?d:.)))", listOf("\r"), listOf("\n"))

        listOf(Regex("(?d)^[a-z]$", RegexOption.MULTILINE), Regex("(?md)^[a-z]$")).forEach { regex ->
            assertFindAll(regex, "a\nb\nc", listOf("a", "b", "c"))
            assertFindAll(regex, "a\nb\r\nc", listOf("a", "c"))
            assertFindAll(regex, "a\u0085b\u2028c\u2029d", emptyList())
        }
        listOf(Regex("(?-d)^[a-z]$", RegexOption.MULTILINE), Regex("(?m-d)^[a-z]$")).forEach { regex ->
            assertFindAll(regex, "a\nb\nc", listOf("a", "b", "c"))
            assertFindAll(regex, "a\nb\r\nc", listOf("a", "b", "c"))
            assertFindAll(regex, "a\u0085b\u2028c\u2029d", listOf("a", "b", "c", "d"))
        }
    }

    @Test fun testInlineUnicodeCaseFlag() {
        // That's a gray area: we don't have an option, but somehow support the flag
        assertFalse(Regex("þ").matches("Þ"))
        assertTrue(Regex("(?i)þ").matches("Þ"))
        assertTrue(Regex("(?iu)þ").matches("Þ"))
        // TODO: Ignored: KT-51859
        // assertFalse(Regex("(?i-u)þ").matches("Þ"))
    }

    @Test fun testInlineFlagsAreNonCapturingGroups() {
        val regex = Regex("(?i:a)")
        val match = regex.matchEntire("A")
        assertNotNull(match)
        assertEquals(1, match.groups.size)
    }

    @Test fun testMisc() {
        var regex: Regex

        // Test (?>...)
        regex = Regex("(?>a*)bb")
        assertFalse(regex.matches("aaabc"))
        assertTrue(regex.matches("aaabb"))
        assertTrue(regex.matches("bb"))
        regex = Regex("(?>a*)abb")
        // Note that a regular RE, like (a*)abb would match it
        assertFalse(regex.matches("aaabb"))

        // Test \Q, \E
        regex = Regex("[a-z]+;\\Q[a-z]+;\\Q(foo.*);\\E[0-9]+")
        assertTrue(regex.matches("abc;[a-z]+;\\Q(foo.*);411"))
        assertFalse(regex.matches("abc;def;foo42;555"))
        assertFalse(regex.matches("abc;\\Qdef;\\Qfoo99;\\E123"))

        regex = Regex("[a-z]+;(foo[0-9]-\\Q(...)\\E);[0-9]+")
        val result = regex.matchEntire("abc;foo5-(...);123")
        assertNotNull(result)
        assertEquals("foo5-(...)", result!!.groupValues[1])
        assertFalse(regex.matches("abc;foo9-(xxx);789"))

        regex = Regex("[a-z]+;(bar[0-9]-[a-z\\Q$-\\E]+);[0-9]+")
        assertTrue(regex.matches("abc;bar0-def$-;123"))

        regex = Regex("[a-z]+;(bar[0-9]-[a-z\\Q-$\\E]+);[0-9]+")
        assertTrue(regex.matches("abc;bar0-def$-;123"))

        regex = Regex("[a-z]+;(bar[0-9]-[a-z\\Q[0-9]\\E]+);[0-9]+")
        assertTrue(regex.matches("abc;bar0-def[99]-]0x[;123"));

        regex = Regex("[a-z]+;(bar[0-9]-[a-z\\[0\\-9\\]]+);[0-9]+")
        assertTrue(regex.matches("abc;bar0-def[99]-]0x[;123"))

        // Test #<comment text>
        regex = Regex("(?x)ab# comment\ncd")
        assertTrue(regex.matches("abcd"))
        assertFalse(regex.matches("ab# commentcd"))
    }

    @Test fun testCompile1() {
        val regex = Regex("[0-9A-Za-z][0-9A-Za-z\\x2e\\x3a\\x2d\\x5f]*")
        val name = "iso-8859-1"
        assertTrue(regex.matches(name))
    }

    @Test fun testCompile2() {
        val findString = "\\Qimport\\E"
        val regex = Regex(findString)
        assertTrue(regex in "import a.A;\n\n import b.B;\nclass C {}")
    }

    @Test fun testCompile3() {
        var regex: Regex
        var result: MatchResult?

        regex = Regex("a$")
        result = regex.find("a\n")
        assertNotNull(result)
        assertEquals("a", result!!.value)
        assertNull(result.next())

        regex = Regex("(a$)")
        result = regex.find("a\n")
        assertNotNull(result)
        assertEquals("a", result!!.value)
        assertEquals("a", result.groupValues[1])
        assertNull(result.next())

        regex = Regex("^.*$", RegexOption.MULTILINE)

        result = regex.find("a\n")
        assertNotNull(result)
        assertEquals("a", result!!.value)
        assertNull(result.next())

        result = regex.find("a\nb\n")
        assertNotNull(result)
        assertEquals("a", result!!.value)
        result = result.next()
        assertNotNull(result)
        assertEquals("b", result!!.value)
        assertNull(result.next())

        result = regex.find("a\nb")
        assertNotNull(result)
        assertEquals("a", result!!.value)
        result = result.next()
        assertNotNull(result)
        assertEquals("b", result!!.value)
        assertNull(result.next())

        result = regex.find("\naa\r\nbb\rcc\n\n")
        assertNotNull(result)
        assertTrue(result!!.value == "")
        result = result.next()
        assertNotNull(result)
        assertEquals("aa", result!!.value)
        result = result.next()
        assertNotNull(result)
        assertEquals("bb", result!!.value)
        result = result.next()
        assertNotNull(result)
        assertEquals("cc", result!!.value)
        result = result.next()
        assertNotNull(result)
        assertTrue(result!!.value == "")
        assertNull(result.next())

        result = regex.find("a")
        assertNotNull(result)
        assertEquals("a", result!!.value)
        assertNull(result.next())

        result = regex.find("")
        assertNull(result)

        regex = Regex("^.*$")
        result = regex.find("")
        assertNotNull(result)
        assertTrue(result!!.value == "")
        assertNull(result.next())
    }

    @Test fun testCompile4() {
        val findString = "\\Qpublic\\E"
        val text = StringBuilder("    public class Class {\n" + "    public class Class {")
        val regex = Regex(findString)

        val result = regex.find(text)
        assertNotNull(result)
        assertEquals(4, result!!.range.start)

        // modify text
        text.setLength(0)
        text.append("Text have been changed.")

        assertNull(regex.find(text))
    }

    @Test fun testCompile5() {
        val p = Regex("^[0-9]")
        val s = p.split("12", 0)
        assertEquals("", s[0])
        assertEquals("2", s[1])
        assertEquals(2, s.size)
    }

    // Regression test for KT-80665
    @Test fun testSymbolCategory() {
        val symbols = Regex("\\p{S}+")
        val punctuation = Regex("\\p{P}+")

        val str = "«»" // Pi Pf
        assertTrue(punctuation.matches(str))
        assertFalse(symbols.matches(str))
    }

    // Regression test for KT-80665, KT-80666
    @Test fun testOtherCategory() {
        val other = Regex("\\p{C}")

        val unassignedStr = "\ufdd0"
        val wsStr = "\t"

        assertTrue(other.matches(unassignedStr), "${other}.matches(\"$unassignedStr\")")
        assertTrue(other.matches(wsStr), "${other}.matches(\"$wsStr\")")

        val unassigned = Regex("\\p{Cn}")
        assertTrue(unassigned.matches(unassignedStr), "${unassigned}.matches(\"$unassignedStr\")")

        val whiteSpace = Regex("\\p{Cc}")
        assertTrue(whiteSpace.matches(wsStr), "${unassigned}.matches(\"$wsStr\")")
    }

    @Test fun testGeneralCategory() {
        val categoryToRegexes = unicodeCategoryTestData
            .map(UnicodeCategorySample::generalCategory)
            .toSet()
            .associateWith(::categoryPatternVariants)

        for (sample in unicodeCategoryTestData) {
            for (regex in categoryToRegexes[sample.generalCategory]!!) {
                assertTrue(
                    regex.matches(sample.value),
                    "$regex should match \"${sample.value}\" (${sample.value.formatFirstCodePoint()}) from ${sample.generalCategory}"
                )
            }

            for ((otherCategory, otherRegexes) in categoryToRegexes) {
                if (otherCategory == sample.generalCategory) continue
                for (otherRegex in otherRegexes) {
                    assertFalse(
                        otherRegex.matches(sample.value),
                        "$otherRegex should not match \"${sample.value}\" (${sample.value.formatFirstCodePoint()}) from ${sample.generalCategory}"
                    )
                }
            }
        }
    }

    @Test fun testMajorCategory() {
        val categoryToRegexes = unicodeCategoryTestData
            .map(UnicodeCategorySample::majorCategory)
            .toSet()
            .associateWith(::categoryPatternVariants)

        for (sample in unicodeCategoryTestData) {
            for (regex in categoryToRegexes[sample.majorCategory]!!) {
                assertTrue(regex.matches(sample.value), "$regex should match \"${sample.value}\" (${sample.value.formatFirstCodePoint()})")
            }

            // Check that regexes for all other categories won't match it
            for ((otherCategory, otherRegexes) in categoryToRegexes) {
                if (otherCategory == sample.majorCategory) continue
                for (otherRegex in otherRegexes) {
                    assertFalse(
                        otherRegex.matches(sample.value),
                        "$otherRegex should not match \"${sample.value}\" (${sample.value.formatFirstCodePoint()})"
                    )
                }
            }
        }
    }

    private fun String.formatFirstCodePoint(): String = codePointAt(0).toHexString(unicodeCodePointFmt)

    private fun categoryPatternVariants(category: String): List<Regex> = listOf(
        Regex("\\p{$category}"),
        Regex("\\p{Is$category}")
    )

    private val unicodeCodePointFmt = HexFormat {
        number {
            prefix = "\\u"
            minLength = 4
            removeLeadingZeros = true
        }
    }

    private data class UnicodeCategorySample(val value: String, val majorCategory: String, val generalCategory: String)

    // Refer to https://www.unicode.org/Public/UCD/latest/ucd/UnicodeData.txt
    private val unicodeCategoryTestData = listOf(
        // Lu, Ll, Lt, Lm, Lo
        UnicodeCategorySample("a", "L", "Ll"),
        UnicodeCategorySample("\u02b6", "L", "Lm"), // ʶ
        UnicodeCategorySample("\u01bb", "L", "Lo"), // ƻ
        UnicodeCategorySample("\u01c5", "L", "Lt"), // ǅ
        UnicodeCategorySample("A", "L", "Lu"),
        // Mn, Me, Mc
        UnicodeCategorySample("\u0334", "M", "Mn"), // ̴
        UnicodeCategorySample("\u0488", "M", "Me"), // ҈
        UnicodeCategorySample("\u0f7f", "M", "Mc"), // ཿ
        // Nd, No, Nl
        UnicodeCategorySample("0", "N", "Nd"),
        UnicodeCategorySample("\u16ef", "N", "Nl"), // ᛯ
        UnicodeCategorySample("\u00be", "N", "No"), // ¾
        // Zl, Zp, Zs
        UnicodeCategorySample("\u2028", "Z", "Zl"),
        UnicodeCategorySample("\u2029", "Z", "Zp"),
        UnicodeCategorySample(" ", "Z", "Zs"),
        // Pc, Pd, Pe, Pf, Pi, Po, Ps
        UnicodeCategorySample("_", "P", "Pc"),
        UnicodeCategorySample("-", "P", "Pd"),
        UnicodeCategorySample("}", "P", "Pe"),
        UnicodeCategorySample("\u00bb", "P", "Pf"), // »
        UnicodeCategorySample("\u00ab", "P", "Pi"), // «
        UnicodeCategorySample("@", "P", "Po"),
        UnicodeCategorySample("{", "P", "Ps"),
        // Sc, Sk, Sm, So
        UnicodeCategorySample("$", "S", "Sc"),
        UnicodeCategorySample("^", "S", "Sk"),
        UnicodeCategorySample("+", "S", "Sm"),
        UnicodeCategorySample("\u00b0", "S", "So"), // °
        // Cc, Cf, Co, Cs, Cn
        UnicodeCategorySample("\r", "C", "Cc"),
        UnicodeCategorySample("\u00ad", "C", "Cf"),
        UnicodeCategorySample("\ue000", "C", "Co"),
        UnicodeCategorySample("\ud800", "C", "Cs"),
        UnicodeCategorySample("\u0378", "C", "Cn"),
    )

    private class UBInfo(var low: Int, var high: Int, var name: String)

    // A table representing the unicode categories
    // private static UBInfo[] UCategories = {
    // Lu
    // Ll
    // Lt
    // Lm
    // Lo
    // Mn
    // Mc
    // Me
    // Nd
    // Nl
    // No
    // Pc
    // Pd
    // Ps
    // Pe
    // Pi
    // Pf
    // Po
    // Sm
    // Sc
    // Sk
    // So
    // Zs
    // Zl
    // Zp
    // Cc
    // Cf
    // Cs
    // Co
    // Cn
    // };

    // A table representing the unicode character blocks
    private val UBlocks = arrayOf(
            /* 0000; 007F; Basic Latin */
            UBInfo(0x0000, 0x007F, "BasicLatin"), // Character.UnicodeBlock.BASIC_LATIN
            /* 0080; 00FF; Latin-1 Supplement */
            UBInfo(0x0080, 0x00FF, "Latin-1Supplement"), // Character.UnicodeBlock.LATIN_1_SUPPLEMENT
            /* 0100; 017F; Latin Extended-A */
            UBInfo(0x0100, 0x017F, "LatinExtended-A"), // Character.UnicodeBlock.LATIN_EXTENDED_A
            /* 0180; 024F; Latin Extended-B */
            // new UBInfo (0x0180,0x024F,"InLatinExtended-B"), //
            // Character.UnicodeBlock.LATIN_EXTENDED_B
            /* 0250; 02AF; IPA Extensions */
            UBInfo(0x0250, 0x02AF, "IPAExtensions"), // Character.UnicodeBlock.IPA_EXTENSIONS
            /* 02B0; 02FF; Spacing Modifier Letters */
            UBInfo(0x02B0, 0x02FF, "SpacingModifierLetters"), // Character.UnicodeBlock.SPACING_MODIFIER_LETTERS
            /* 0300; 036F; Combining Diacritical Marks */
            UBInfo(0x0300, 0x036F, "CombiningDiacriticalMarks"), // Character.UnicodeBlock.COMBINING_DIACRITICAL_MARKS
            /* 0370; 03FF; Greek */
            UBInfo(0x0370, 0x03FF, "Greek"), // Character.UnicodeBlock.GREEK
            /* 0400; 04FF; Cyrillic */
            UBInfo(0x0400, 0x04FF, "Cyrillic"), // Character.UnicodeBlock.CYRILLIC
            /* 0530; 058F; Armenian */
            UBInfo(0x0530, 0x058F, "Armenian"), // Character.UnicodeBlock.ARMENIAN
            /* 0590; 05FF; Hebrew */
            UBInfo(0x0590, 0x05FF, "Hebrew"), // Character.UnicodeBlock.HEBREW
            /* 0600; 06FF; Arabic */
            UBInfo(0x0600, 0x06FF, "Arabic"), // Character.UnicodeBlock.ARABIC
            /* 0700; 074F; Syriac */
            UBInfo(0x0700, 0x074F, "Syriac"), // Character.UnicodeBlock.SYRIAC
            /* 0780; 07BF; Thaana */
            UBInfo(0x0780, 0x07BF, "Thaana"), // Character.UnicodeBlock.THAANA
            /* 0900; 097F; Devanagari */
            UBInfo(0x0900, 0x097F, "Devanagari"), // Character.UnicodeBlock.DEVANAGARI
            /* 0980; 09FF; Bengali */
            UBInfo(0x0980, 0x09FF, "Bengali"), // Character.UnicodeBlock.BENGALI
            /* 0A00; 0A7F; Gurmukhi */
            UBInfo(0x0A00, 0x0A7F, "Gurmukhi"), // Character.UnicodeBlock.GURMUKHI
            /* 0A80; 0AFF; Gujarati */
            UBInfo(0x0A80, 0x0AFF, "Gujarati"), // Character.UnicodeBlock.GUJARATI
            /* 0B00; 0B7F; Oriya */
            UBInfo(0x0B00, 0x0B7F, "Oriya"), // Character.UnicodeBlock.ORIYA
            /* 0B80; 0BFF; Tamil */
            UBInfo(0x0B80, 0x0BFF, "Tamil"), // Character.UnicodeBlock.TAMIL
            /* 0C00; 0C7F; Telugu */
            UBInfo(0x0C00, 0x0C7F, "Telugu"), // Character.UnicodeBlock.TELUGU
            /* 0C80; 0CFF; Kannada */
            UBInfo(0x0C80, 0x0CFF, "Kannada"), // Character.UnicodeBlock.KANNADA
            /* 0D00; 0D7F; Malayalam */
            UBInfo(0x0D00, 0x0D7F, "Malayalam"), // Character.UnicodeBlock.MALAYALAM
            /* 0D80; 0DFF; Sinhala */
            UBInfo(0x0D80, 0x0DFF, "Sinhala"), // Character.UnicodeBlock.SINHALA
            /* 0E00; 0E7F; Thai */
            UBInfo(0x0E00, 0x0E7F, "Thai"), // Character.UnicodeBlock.THAI
            /* 0E80; 0EFF; Lao */
            UBInfo(0x0E80, 0x0EFF, "Lao"), // Character.UnicodeBlock.LAO
            /* 0F00; 0FFF; Tibetan */
            UBInfo(0x0F00, 0x0FFF, "Tibetan"), // Character.UnicodeBlock.TIBETAN
            /* 1000; 109F; Myanmar */
            UBInfo(0x1000, 0x109F, "Myanmar"), // Character.UnicodeBlock.MYANMAR
            /* 10A0; 10FF; Georgian */
            UBInfo(0x10A0, 0x10FF, "Georgian"), // Character.UnicodeBlock.GEORGIAN
            /* 1100; 11FF; Hangul Jamo */
            UBInfo(0x1100, 0x11FF, "HangulJamo"), // Character.UnicodeBlock.HANGUL_JAMO
            /* 1200; 137F; Ethiopic */
            UBInfo(0x1200, 0x137F, "Ethiopic"), // Character.UnicodeBlock.ETHIOPIC
            /* 13A0; 13FF; Cherokee */
            UBInfo(0x13A0, 0x13FF, "Cherokee"), // Character.UnicodeBlock.CHEROKEE
            /* 1400; 167F; Unified Canadian Aboriginal Syllabics */
            UBInfo(0x1400, 0x167F, "UnifiedCanadianAboriginalSyllabics"), // Character.UnicodeBlock.UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS
            /* 1680; 169F; Ogham */
            UBInfo(0x1680, 0x169F, "Ogham"), // Character.UnicodeBlock.OGHAM
            /* 16A0; 16FF; Runic */
            UBInfo(0x16A0, 0x16FF, "Runic"), // Character.UnicodeBlock.RUNIC
            /* 1780; 17FF; Khmer */
            UBInfo(0x1780, 0x17FF, "Khmer"), // Character.UnicodeBlock.KHMER
            /* 1800; 18AF; Mongolian */
            UBInfo(0x1800, 0x18AF, "Mongolian"), // Character.UnicodeBlock.MONGOLIAN
            /* 1E00; 1EFF; Latin Extended Additional */
            UBInfo(0x1E00, 0x1EFF, "LatinExtendedAdditional"), // Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL
            /* 1F00; 1FFF; Greek Extended */
            UBInfo(0x1F00, 0x1FFF, "GreekExtended"), // Character.UnicodeBlock.GREEK_EXTENDED
            /* 2000; 206F; General Punctuation */
            UBInfo(0x2000, 0x206F, "GeneralPunctuation"), // Character.UnicodeBlock.GENERAL_PUNCTUATION
            /* 2070; 209F; Superscripts and Subscripts */
            UBInfo(0x2070, 0x209F, "SuperscriptsandSubscripts"), // Character.UnicodeBlock.SUPERSCRIPTS_AND_SUBSCRIPTS
            /* 20A0; 20CF; Currency Symbols */
            UBInfo(0x20A0, 0x20CF, "CurrencySymbols"), // Character.UnicodeBlock.CURRENCY_SYMBOLS
            /* 20D0; 20FF; Combining Marks for Symbols */
            UBInfo(0x20D0, 0x20FF, "CombiningMarksforSymbols"), // Character.UnicodeBlock.COMBINING_MARKS_FOR_SYMBOLS
            /* 2100; 214F; Letterlike Symbols */
            UBInfo(0x2100, 0x214F, "LetterlikeSymbols"), // Character.UnicodeBlock.LETTERLIKE_SYMBOLS
            /* 2150; 218F; Number Forms */
            UBInfo(0x2150, 0x218F, "NumberForms"), // Character.UnicodeBlock.NUMBER_FORMS
            /* 2190; 21FF; Arrows */
            UBInfo(0x2190, 0x21FF, "Arrows"), // Character.UnicodeBlock.ARROWS
            /* 2200; 22FF; Mathematical Operators */
            UBInfo(0x2200, 0x22FF, "MathematicalOperators"), // Character.UnicodeBlock.MATHEMATICAL_OPERATORS
            /* 2300; 23FF; Miscellaneous Technical */
            UBInfo(0x2300, 0x23FF, "MiscellaneousTechnical"), // Character.UnicodeBlock.MISCELLANEOUS_TECHNICAL
            /* 2400; 243F; Control Pictures */
            UBInfo(0x2400, 0x243F, "ControlPictures"), // Character.UnicodeBlock.CONTROL_PICTURES
            /* 2440; 245F; Optical Character Recognition */
            UBInfo(0x2440, 0x245F, "OpticalCharacterRecognition"), // Character.UnicodeBlock.OPTICAL_CHARACTER_RECOGNITION
            /* 2460; 24FF; Enclosed Alphanumerics */
            UBInfo(0x2460, 0x24FF, "EnclosedAlphanumerics"), // Character.UnicodeBlock.ENCLOSED_ALPHANUMERICS
            /* 2500; 257F; Box Drawing */
            UBInfo(0x2500, 0x257F, "BoxDrawing"), // Character.UnicodeBlock.BOX_DRAWING
            /* 2580; 259F; Block Elements */
            UBInfo(0x2580, 0x259F, "BlockElements"), // Character.UnicodeBlock.BLOCK_ELEMENTS
            /* 25A0; 25FF; Geometric Shapes */
            UBInfo(0x25A0, 0x25FF, "GeometricShapes"), // Character.UnicodeBlock.GEOMETRIC_SHAPES
            /* 2600; 26FF; Miscellaneous Symbols */
            UBInfo(0x2600, 0x26FF, "MiscellaneousSymbols"), // Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS
            /* 2700; 27BF; Dingbats */
            UBInfo(0x2700, 0x27BF, "Dingbats"), // Character.UnicodeBlock.DINGBATS
            /* 2800; 28FF; Braille Patterns */
            UBInfo(0x2800, 0x28FF, "BraillePatterns"), // Character.UnicodeBlock.BRAILLE_PATTERNS
            /* 2E80; 2EFF; CJK Radicals Supplement */
            UBInfo(0x2E80, 0x2EFF, "CJKRadicalsSupplement"), // Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT
            /* 2F00; 2FDF; Kangxi Radicals */
            UBInfo(0x2F00, 0x2FDF, "KangxiRadicals"), // Character.UnicodeBlock.KANGXI_RADICALS
            /* 2FF0; 2FFF; Ideographic Description Characters */
            UBInfo(0x2FF0, 0x2FFF, "IdeographicDescriptionCharacters"), // Character.UnicodeBlock.IDEOGRAPHIC_DESCRIPTION_CHARACTERS
            /* 3000; 303F; CJK Symbols and Punctuation */
            UBInfo(0x3000, 0x303F, "CJKSymbolsandPunctuation"), // Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            /* 3040; 309F; Hiragana */
            UBInfo(0x3040, 0x309F, "Hiragana"), // Character.UnicodeBlock.HIRAGANA
            /* 30A0; 30FF; Katakana */
            UBInfo(0x30A0, 0x30FF, "Katakana"), // Character.UnicodeBlock.KATAKANA
            /* 3100; 312F; Bopomofo */
            UBInfo(0x3100, 0x312F, "Bopomofo"), // Character.UnicodeBlock.BOPOMOFO
            /* 3130; 318F; Hangul Compatibility Jamo */
            UBInfo(0x3130, 0x318F, "HangulCompatibilityJamo"), // Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
            /* 3190; 319F; Kanbun */
            UBInfo(0x3190, 0x319F, "Kanbun"), // Character.UnicodeBlock.KANBUN
            /* 31A0; 31BF; Bopomofo Extended */
            UBInfo(0x31A0, 0x31BF, "BopomofoExtended"), // Character.UnicodeBlock.BOPOMOFO_EXTENDED
            /* 3200; 32FF; Enclosed CJK Letters and Months */
            UBInfo(0x3200, 0x32FF, "EnclosedCJKLettersandMonths"), // Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS
            /* 3300; 33FF; CJK Compatibility */
            UBInfo(0x3300, 0x33FF, "CJKCompatibility"), // Character.UnicodeBlock.CJK_COMPATIBILITY
            /* 3400; 4DB5; CJK Unified Ideographs Extension A */
            UBInfo(0x3400, 0x4DB5, "CJKUnifiedIdeographsExtensionA"), // Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            /* 4E00; 9FFF; CJK Unified Ideographs */
            UBInfo(0x4E00, 0x9FFF, "CJKUnifiedIdeographs"), // Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            /* A000; A48F; Yi Syllables */
            UBInfo(0xA000, 0xA48F, "YiSyllables"), // Character.UnicodeBlock.YI_SYLLABLES
            /* A490; A4CF; Yi Radicals */
            UBInfo(0xA490, 0xA4CF, "YiRadicals"), // Character.UnicodeBlock.YI_RADICALS
            /* AC00; D7A3; Hangul Syllables */
            UBInfo(0xAC00, 0xD7A3, "HangulSyllables"), // Character.UnicodeBlock.HANGUL_SYLLABLES
            /* D800; DB7F; High Surrogates */
            /* DB80; DBFF; High Private Use Surrogates */
            /* DC00; DFFF; Low Surrogates */
            /* E000; F8FF; Private Use */
            /* F900; FAFF; CJK Compatibility Ideographs */
            UBInfo(0xF900, 0xFAFF, "CJKCompatibilityIdeographs"), // Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            /* FB00; FB4F; Alphabetic Presentation Forms */
            UBInfo(0xFB00, 0xFB4F, "AlphabeticPresentationForms"), // Character.UnicodeBlock.ALPHABETIC_PRESENTATION_FORMS
            /* FB50; FDFF; Arabic Presentation Forms-A */
            UBInfo(0xFB50, 0xFDFF, "ArabicPresentationForms-A"), // Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A
            /* FE20; FE2F; Combining Half Marks */
            UBInfo(0xFE20, 0xFE2F, "CombiningHalfMarks"), // Character.UnicodeBlock.COMBINING_HALF_MARKS
            /* FE30; FE4F; CJK Compatibility Forms */
            UBInfo(0xFE30, 0xFE4F, "CJKCompatibilityForms"), // Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
            /* FE50; FE6F; Small Form Variants */
            UBInfo(0xFE50, 0xFE6F, "SmallFormVariants"), // Character.UnicodeBlock.SMALL_FORM_VARIANTS
            /* FE70; FEFE; Arabic Presentation Forms-B */
            // new UBInfo (0xFE70,0xFEFE,"InArabicPresentationForms-B"), //
            // Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
            /* FEFF; FEFF; Specials */
            UBInfo(0xFEFF, 0xFEFF, "Specials"), // Character.UnicodeBlock.SPECIALS
            /* FF00; FFEF; Halfwidth and Fullwidth Forms */
            UBInfo(0xFF00, 0xFFEF, "HalfwidthandFullwidthForms"), // Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
            /* FFF0; FFFD; Specials */
            UBInfo(0xFFF0, 0xFFFD, "Specials") // Character.UnicodeBlock.SPECIALS
    )
}
