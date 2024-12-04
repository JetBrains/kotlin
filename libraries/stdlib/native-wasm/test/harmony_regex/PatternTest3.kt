/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text.harmony_regex

import kotlin.test.*

class PatternTest3 {
    @Test fun escapeStringInCommentsMode() {
        // Q and E immediately follow backslash
        " [ a - z ] \\Q [ A - Z ] \\E [ a - z ] ".toRegex(RegexOption.COMMENTS).let { regex ->
            assertTrue(regex.matches("a [ A - Z ] b"))
        }
        // \Q is separated and the backslash quotes the following char - the space, thus escape mode is not enabled.
        // \E doesn't disable any escape mode, leading to the error.
        assertFailsWith<IllegalArgumentException> {
            " [ a - z ] \\ Q [ A - Z ] \\E [ a - z ] ".toRegex(RegexOption.COMMENTS) // Illegal/unsupported escape char E
        }
        // \E is separated, thus escape mode stands enabled till the end of pattern string.
        " [ a - z ] \\Q [ A - Z ] \\ E [ a - z ] ".toRegex(RegexOption.COMMENTS).let { regex ->
            assertTrue(regex.matches("a [ A - Z ] \\ E [ a - z ] "))
        }
        // both \Q and \E are separated, and the backslash quotes the following char - the space.
        " [ a - z ] \\ Q [ A - Z ] \\  E [ a - z ] ".toRegex(RegexOption.COMMENTS).let { regex ->
            assertTrue(regex.matches("a QB Ec"))
        }
    }

    @Test fun specialConstructsInCommentsMode() {
        // positive lookbehind
        assertFailsWith<IllegalArgumentException> {
            "(? <=[a-z])\\d".toRegex(RegexOption.COMMENTS) // (?<=X) - ?< shouldn't be separated
        }
        "(?< =[a-z])\\d".toRegex(RegexOption.COMMENTS).let { regex ->
            assertEquals("4", regex.find("...a4...B1")?.value)
        }
        // negative lookbehind
        assertFailsWith<IllegalArgumentException> {
            "(? <![a-z])\\d".toRegex(RegexOption.COMMENTS) // (?<=X) - ?< shouldn't be separated
        }
        "(?< ![a-z])\\d".toRegex(RegexOption.COMMENTS).let { regex ->
            assertEquals("1", regex.find("...a4...B1")?.value)
        }
        // positive lookahead
        assertFailsWith<IllegalArgumentException> {
            "[a-z](? =\\d)".toRegex(RegexOption.COMMENTS) // (?=X) - ?= shouldn't be separated
        }
        // negative lookahead
        assertFailsWith<IllegalArgumentException> {
            "[a-z](? !\\d)".toRegex(RegexOption.COMMENTS) // (?!X) - ?! shouldn't be separated
        }
    }

    @Test fun matchNamedGroupInCommentsMode() {
        assertFailsWith<IllegalArgumentException> {
            "(? <first>\\d+)".toRegex(RegexOption.COMMENTS) // (?<name>X) - ?< shouldn't be separated
        }
        "( ?< first  > \\d + ) - (?< se c  ond >\\d+)".toRegex(RegexOption.COMMENTS).let { regex ->
            val match = regex.find("123-456")!!
            assertEquals("123-456", match.value)
            assertEquals("123", match.groups["first"]?.value)
            assertEquals("456", match.groups["second"]?.value)
        }
    }

    @Test fun matchBackReferenceInCommentsMode() {
        "(?<first>\\d+)-\\ k<first>".toRegex(RegexOption.COMMENTS).let { regex ->
            assertTrue(regex.matches("123- k<first>")) // \k<name> - \k shouldn't be separated. Otherwise, backslash quotes the following char - the space
        }
        "(\\d+)-\\    1".toRegex(RegexOption.COMMENTS).let { regex ->
            assertTrue(regex.matches("123- 1")) // \n - the construct shouldn't be separated. Otherwise, backslash quotes the following char - the space
        }
        "(?<first>\\d+)-\\k  < fi r  st > ".toRegex(RegexOption.COMMENTS).let { regex ->
            val match = regex.find("123-123")!!
            assertEquals("123-123", match.value)
            assertEquals("123", match.groups["first"]?.value)
        }
        "0(1(2(3(4(5(6(7(8(9(A(B(C))))))))))))\\1  1 ".toRegex(RegexOption.COMMENTS).let { regex ->
            val match = regex.find("0123456789ABCBC")!!
            assertEquals("BC", match.groups[11]?.value)
            assertEquals("56789ABC", match.groups[5]?.value)
        }
    }
}