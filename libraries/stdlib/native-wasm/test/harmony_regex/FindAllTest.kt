/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package test.text.harmony_regex

import kotlin.text.*
import kotlin.test.*

class FindAllTest {

    internal fun Regex.allGroups(text: String) =
            findAll(text).map {
                it.groups.mapIndexed { index, it ->
                    "$index => ${it?.value}"
                }.joinToString("; ")
            }.toList()

    /**
     * Tests regular expressions with lookbehind asserts.
     */
    @Test fun testLookBehind() {
        var regex: Regex
        var result: List<String>

        regex = "(?<=^/nl(?:/nl)?/\\d{1,600}[\\d+]{0,600}/[\\d+]{0,600})(\\d+)".toRegex()
        result = regex.allGroups("/nl/nl/1+2/3+4/")
        assertEquals(2, result.count())
        assertEquals("0 => 3; 1 => 3", result[0])
        assertEquals("0 => 4; 1 => 4", result[1])

        regex = "abe(?<=[ab][!be](.|\\b))(=|t)".toRegex()
        result = regex.allGroups("abet   abe=")
        assertEquals(2, result.count())
        assertEquals("0 => abet; 1 => e; 2 => t", result[0])
        assertEquals("0 => abe=; 1 => ; 2 => =", result[1])
    }

    /**
     * Tests regular expressions with lookahead asserts.
     */
    @Test fun testLookAheadBehind() {
        var regex: Regex
        var result: List<String>

        regex = "a(?=b?)(\\w|)c".toRegex()
        result = regex.allGroups("abcfgac")
        assertEquals(2, result.count())
        assertEquals("0 => abc; 1 => b", result[0])
        assertEquals("0 => ac; 1 => ", result[1])

        regex = "[a!](?=d|&)\\b[&d]".toRegex()
        result = regex.allGroups("ada& !d!&")
        assertEquals(2, result.count())
        assertEquals("0 => a&", result[0])
        assertEquals("0 => !d", result[1])

        regex = "(?=ab)(a|^)b".toRegex()
        result = regex.allGroups("abcab")
        assertEquals(2, result.count())
        assertEquals("0 => ab; 1 => a", result[0])
        assertEquals("0 => ab; 1 => a", result[1])

        regex = "(?=[a-k][a-z])(?=[a-d][c-x])[d-y][x-z]".toRegex()
        result = regex.allGroups("abdydx")
        assertEquals(1, result.count())
        assertEquals("0 => dx", result[0])
    }
}
