/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

class StringBuilderJVMTest {
    // KT-52336
    // Tests that the deprecated Common StringBuilder.append(CharArray, Int, Int) does not affect JVM target.
    @Test
    fun deprecatedAppend() {
        val chars = charArrayOf('a', 'b', 'c', 'd')
        val sb = StringBuilder()
        sb.append(chars, 1, 2)
        assertEquals("bc", sb.toString())
    }
}