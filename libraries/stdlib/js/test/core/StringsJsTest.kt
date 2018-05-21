/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.js

import kotlin.test.*

class StringsJsTest {

    @Test fun charArrayFullSlice() {
        val chars = CharArray(5, { i -> 'a' + i })
        assertEquals("abcde", String(chars, 0, chars.size))
    }

    @Test fun charArraySlice() {
        val chars = CharArray(5, { i -> 'a' + i })
        assertEquals("cd", String(chars, 2, 2))
    }
}