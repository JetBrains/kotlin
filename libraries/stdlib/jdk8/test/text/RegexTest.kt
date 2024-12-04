/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.text.test

import org.junit.Test
import kotlin.test.*

class RegexTest {
    @Test fun namedGroups() {
        val input = "1a 2b 3"
        val regex = "(?<num>\\d)(?<liter>\\w)?".toRegex()

        val matches = regex.findAll(input).toList()
        assertTrue(matches.all { it.groups.size == 3 })
        val (m1, m2, m3) = matches

        assertEquals("1", m1.groups["num"]?.value)
        assertEquals(0..0, m1.groups["num"]?.range)
        assertEquals("a", m1.groups["liter"]?.value)
        assertEquals(1..1, m1.groups["liter"]?.range)

        assertEquals("2", m2.groups["num"]?.value)
        assertEquals(3..3, m2.groups["num"]?.range)
        assertEquals("b", m2.groups["liter"]?.value)
        assertEquals(4..4, m2.groups["liter"]?.range)

        assertEquals("3", m3.groups["num"]?.value)
        assertNull(m3.groups["liter"])

        assertFailsWith<IllegalArgumentException> { m2.groups["unknown_group"] }.let { e ->
            assertTrue("unknown_group" in e.message!!)
        }
    }

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
