/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.sourceMap

import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.ParsingResult.Success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Base64VLQTest {
    private fun encodeUnsigned(value: Int): String =
        StringBuilder().also { Base64VLQ.encodeUnsigned(it, value) }.toString()

    private fun encodeSigned(value: Int): String =
        StringBuilder().also { Base64VLQ.encode(it, value) }.toString()

    @Test
    fun `encodeUnsigned single digit values`() {
        // Base64 alphabet: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        assertEquals("A", encodeUnsigned(0))
        assertEquals("B", encodeUnsigned(1))
        assertEquals("f", encodeUnsigned(31))
    }

    @Test
    fun `encodeUnsigned requires continuation digit at base boundary`() {
        // 32 no longer fits into a single 5-bit digit, so a continuation digit is required.
        assertEquals("gB", encodeUnsigned(32))
    }

    @Test
    fun `encodeUnsigned differs from signed encode due to no sign doubling`() {
        // encode(1) doubles and sets the sign bit (toVLQSigned(1) = 2), while encodeUnsigned(1) doesn't.
        assertNotEquals(encodeSigned(1), encodeUnsigned(1))
        assertEquals("C", encodeSigned(1))
        assertEquals("B", encodeUnsigned(1))
    }

    @Test
    fun `encodeUnsigned round trips through the unsigned VLQ decode path`() {
        // Builds a minimal "scopes" string (per the ECMA-426 scopes extension grammar) using encodeUnsigned
        // for the scope flags/line/column fields, then decodes it back and checks the values survive.
        // OriginalScopeTree : OriginalScopeStart `,` OriginalScopeEnd
        // OriginalScopeStart : `B` ScopeFlags ScopeLine ScopeColumn
        // OriginalScopeEnd : `C` ScopeLine ScopeColumn
        val isStackFrameFlag = 0x4
        val startLine = 5
        val startColumn = 10
        val endLineIncrement = 3
        val endColumn = 2

        val scopes = buildString {
            append('B')
            Base64VLQ.encodeUnsigned(this, isStackFrameFlag)
            Base64VLQ.encodeUnsigned(this, startLine)
            Base64VLQ.encodeUnsigned(this, startColumn)
            append(',')
            append('C')
            Base64VLQ.encodeUnsigned(this, endLineIncrement)
            Base64VLQ.encodeUnsigned(this, endColumn)
        }

        val result = ECMA426BasedSourceMapParser.decodeSourceScopes(scopes, names = emptyList())
        val scopeRecords = (result as Success).value

        assertEquals(1, scopeRecords.size)
        val scope = scopeRecords.single()
        assertTrue(scope.isStackFrame)
        assertEquals(startLine.toUInt(), scope.start.line)
        assertEquals(startColumn.toUInt(), scope.start.column)
        assertEquals((startLine + endLineIncrement).toUInt(), scope.end.line)
        assertEquals(endColumn.toUInt(), scope.end.column)
    }

    @Test
    fun `encodeUnsigned round trips large values requiring multiple continuation digits`() {
        val isStackFrameFlag = 0x4
        val startLine = 0
        val largeColumn = 1_000_000

        val scopes = buildString {
            append('B')
            Base64VLQ.encodeUnsigned(this, isStackFrameFlag)
            Base64VLQ.encodeUnsigned(this, startLine)
            Base64VLQ.encodeUnsigned(this, largeColumn)
            append(',')
            append('C')
            Base64VLQ.encodeUnsigned(this, 0)
            Base64VLQ.encodeUnsigned(this, 0)
        }

        val result = ECMA426BasedSourceMapParser.decodeSourceScopes(scopes, names = emptyList())
        val scope = (result as Success).value.single()

        assertEquals(largeColumn.toUInt(), scope.start.column)
    }
}
