/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text
import kotlin.test.*
import kotlin.text.unicode.*

@OptIn(ExperimentalUnicodeApi::class)
class CodePointCasingJVMTest {

    @Test
    fun caseAssumptions() {
        // each BMP char title case conversion results in another BMP char
        for (code in 0..Char.MAX_VALUE.code) {
            assertTrue(Character.toTitleCase(code) <= Char.MAX_VALUE.code)
        }
        // characters in supplementary planes do not have special title case conversions and do not have one-to-many upper/lowercase expansions
        for (code in Char.MAX_VALUE.code + 1..CodePoint.MAX_VALUE.code) {
            assertEquals(Character.toUpperCase(code), Character.toTitleCase(code))
            val string = Character.toChars(code).concatToString()
            assertEquals(1, string.uppercase().codePointCount())
            assertEquals(1, string.lowercase().codePointCount())
        }
    }

}
