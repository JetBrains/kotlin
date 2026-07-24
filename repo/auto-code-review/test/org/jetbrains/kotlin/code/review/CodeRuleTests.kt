/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeRuleTests {
    private fun dummyRule(source: String, vararg patterns: String) = CodeRule(
        name = "Dummy",
        text = "Dummy",
        patterns = CodeRulePatterns(patterns.toList()),
        source = ProjectFilePath(source)
    )

    @Test
    fun `pattern matches full file path`() {
        val rule = dummyRule("compiler/ir/code-rules.md", "compiler/ir/ir.tree/src/**/*.kt")

        assertTrue(rule.patternsMatch(ProjectFilePath("compiler/ir/ir.tree/src/org/jetbrains/foo.kt")))
    }

    @Test
    fun `pattern matches relative file path`() {
        val rule = dummyRule("compiler/ir/code-rules.md", "ir.tree/src/**/*.kt")
        assertTrue(rule.patternsMatch(ProjectFilePath("compiler/ir/ir.tree/src/org/jetbrains/foo.kt")))
    }

    @Test
    fun `pattern doesn't match other file path`() {
        val rule = dummyRule("compiler/ir/code-rules.md", "ir.tree/src/**/*.kt")
        assertFalse(rule.patternsMatch(ProjectFilePath("kotlin-native/ir.tree/src/org/jetbrains/foo.kt")))
    }
}
