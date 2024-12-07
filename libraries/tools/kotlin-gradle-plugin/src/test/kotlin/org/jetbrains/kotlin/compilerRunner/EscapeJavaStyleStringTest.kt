/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.junit.Test
import kotlin.test.DefaultAsserter.assertEquals

class EscapeJavaStyleStringTest {

    @Test
    fun testEscapeJavaStyleString() {
        assertEscapeJava("", "", "empty string")
        assertEscapeJava("foo", "foo")
        assertEscapeJava("\\t", "\t", "tab")
        assertEscapeJava("\\\\", "\\", "backslash")
        assertEscapeJava("'", "'", "single quote should not be escaped")
        assertEscapeJava("\\\\\\b\\t\\r", "\\\b\t\r")
        assertEscapeJava("\\u1234", "\u1234")
        assertEscapeJava("\\u0234", "\u0234")
        assertEscapeJava("\\u00EF", "\u00ef")
        assertEscapeJava("\\u0001", "\u0001")
        assertEscapeJava(
            "\\uABCD",
            "\uabcd",
            "Should use capitalized Unicode hex"
        )

        assertEscapeJava(
            "He didn't say, \\\"stop!\\\"",
            "He didn't say, \"stop!\""
        )
        assertEscapeJava(
            "This space is non-breaking:" + "\\u00A0",
            "This space is non-breaking:\u00a0",
            "non-breaking space"
        )
        assertEscapeJava(
            "\\uABCD\\u1234\\u012C",
            "\uABCD\u1234\u012C"
        )
    }

    private fun assertEscapeJava(
        expected: String,
        original: String,
        message: String? = null
    ) {
        val converted: String = original.escapeJavaStyleString()
        val assertMessage = "escapeJava(String) failed" + if (message == null) "" else ": $message"
        assertEquals(assertMessage, expected, converted)
    }
}