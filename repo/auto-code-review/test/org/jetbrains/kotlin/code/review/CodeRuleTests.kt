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

    private fun CodeRule.matches(path: String, dir: String = source.dir.pathFromProjectRoot): Boolean =
        patternsMatch(ProjectFilePath(path), ProjectDirPath(dir))

    @Test
    fun `pattern matches relative file path`() {
        val rule = dummyRule("compiler/ir/code-rules.md", "ir.tree/src/**/*.kt")
        assertTrue(rule.matches("compiler/ir/ir.tree/src/org/jetbrains/foo.kt"))
        assertFalse(rule.matches("compiler/ir/backend/ir.tree/src/org/jetbrains/foo.kt"))
    }

    @Test
    fun `pattern doesn't match file path from project root`() {
        val rule = dummyRule("compiler/ir/code-rules.md", "compiler/ir/ir.tree/src/**/*.kt")
        assertFalse(rule.matches("compiler/ir/ir.tree/src/org/jetbrains/foo.kt"))
    }

    @Test
    fun `pattern matches file path relative to given directory`() {
        val rule = dummyRule("compiler/ir/code-rules.md", "src")
        assertTrue(rule.matches("kotlin-native/backend/src/foo.kt", dir = "kotlin-native/backend"))
        assertFalse(rule.matches("kotlin-native/backend/test/foo.kt", dir = "kotlin-native/backend"))

        // Enclosing directories of the given directory don't matter:
        assertFalse(rule.matches("kotlin-native/src/backend/foo.kt", dir = "kotlin-native/src/backend"))
    }
}
