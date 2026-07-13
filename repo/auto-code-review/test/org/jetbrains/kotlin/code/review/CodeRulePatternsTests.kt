/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeRulePatternsTests {
    private fun matches(path: String, patterns: List<String>): Boolean {
        return CodeRulePatterns(patterns).match(path)
    }

    private fun assertMatches(path: String, vararg patterns: String) {
        assertTrue(matches(path, patterns.toList()))
    }

    private fun assertDoesNotMatch(path: String, vararg patterns: String) {
        assertFalse(matches(path, patterns.toList()))
    }

    @Test
    fun `matches when no patterns`() {
        assertMatches("sub/dir/file.kt")
    }

    @Test
    fun `matches relative pattern`() {
        assertMatches("sub/dir/file.kt", "dir")
    }

    @Test
    fun `does not match absolute pattern`() {
        assertDoesNotMatch("sub/dir/file.kt", "/dir")
    }

    @Test
    fun `matches absolute pattern`() {
        assertMatches("sub/dir/file.kt", "/sub/dir")
    }

    @Test
    fun `is excluded with relative exclusion pattern`() {
        assertMatches("sub/dir/test/file.kt", "*.kt")
        assertDoesNotMatch("sub/dir/test/file.kt", "*.kt", "!test")
    }

    @Test
    fun `is excluded with absolute exclusion pattern`() {
        assertMatches("sub/dir/test/file.kt", "*.kt")
        assertDoesNotMatch("sub/dir/test/file.kt", "*.kt", "!sub/dir/test")
    }

    @Test
    fun `double wildcard matches zero and more segments`() {
        assertMatches("compiler/ir/backend.jvm/src/foo.kt", "compiler/**/src/**/*.kt")
        assertMatches("compiler/ir/backend.jvm/src/pkg/foo.kt", "compiler/**/src/**/*.kt")
        assertMatches("compiler/ir/backend.jvm/src/pkg/sub/foo.kt", "compiler/**/src/**/*.kt")
    }
}
