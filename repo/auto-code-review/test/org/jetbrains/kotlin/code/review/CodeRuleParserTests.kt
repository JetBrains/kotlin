/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CodeRuleParserTests {
    private fun parse(text: String) = CodeRuleParser.parseRule(
        text.lines(),
        ProjectFilePath("code-rules.md")
    )

    private fun assertRuleIsRejected(text: String, vararg expectedMessageParts: String) {
        val exception = assertFailsWith<IllegalStateException> { parse(text) }
        expectedMessageParts.forEach { assertContains(exception.message.orEmpty(), it) }
    }

    @Test
    fun `exclusion patterns after other patterns are accepted`() {
        val text = """
            # Rule
            
            Applies to:
            ```
            *.kt
            !test
            !testData
            ```
            
            Rule text
        """.trimIndent()

        val rule = parse(text)

        assertEquals(listOf("*.kt", "!test", "!testData"), rule.patterns.patterns)
    }

    @Test
    fun `pattern after exclusion pattern is rejected`() {
        val text = """
            # Rule
            
            Applies to:
            ```
            !test
            *.kt
            ```
            
            Rule text
        """.trimIndent()

        assertRuleIsRejected(
            text,
            "exclusion patterns (!) must go after all other patterns", "*.kt"
        )
    }

    @Test
    fun `patterns in code block`() {
        val text = """
            # Rule
            
            Applies to:
            
            ```text
            *.kt
            
            *.kts
            !test
            ```
            
            Rule text
        """.trimIndent()

        val rule = parse(text)

        assertEquals(listOf("*.kt", "*.kts", "!test"), rule.patterns.patterns)
        assertEquals("Rule text", rule.text)
    }

    @Test
    fun `inline pattern without backticks is rejected`() {
        val text = """
            # Rule
            
            Applies to: src
            
            Rule text
        """.trimIndent()

        assertRuleIsRejected(
            text,
            "expected `Applies to:` to be followed by a single pattern in backticks", "Applies to: src"
        )
    }

    @Test
    fun `multiple inline patterns are rejected`() {
        val text = """
            # Rule
            
            Applies to: `src`, `test`
            
            Rule text
        """.trimIndent()

        assertRuleIsRejected(
            text,
            "expected `Applies to:` to be followed by a single pattern in backticks"
        )
    }

    @Test
    fun `label without code block is rejected`() {
        val text = """
            # Rule
            
            Applies to:
            src
            
            Rule text
        """.trimIndent()

        assertRuleIsRejected(
            text,
            "expected a code block with patterns (```) right after `Applies to:`", "src"
        )
    }

    @Test
    fun `unclosed code block is rejected`() {
        val text = """
            # Rule
            
            Applies to:
            ```
            src
            
            Rule text
        """.trimIndent()

        assertRuleIsRejected(
            text,
            "the code block with patterns is not closed"
        )
    }

    @Test
    fun `empty code block is rejected`() {
        val text = """
            # Rule
            
            Applies to:
            ```
            
            ```
            
            Rule text
        """.trimIndent()

        assertRuleIsRejected(
            text,
            "the code block with patterns is empty"
        )
    }

    @Test
    fun `rule without label is rejected`() {
        val text = """
            # Rule
            
            Rule text
        """.trimIndent()

        assertRuleIsRejected(
            text,
            "expected `Applies to:` right after the rule name", "Rule text"
        )
    }

    @Test
    fun `label after rule text is rejected`() {
        val text = """
            # Rule
            
            Applies to: `test`

            Rule text
            
            Applies to: `src`
        """.trimIndent()

        assertRuleIsRejected(
            text,
            "`Applies to:` is allowed only right after the rule name", "Applies to: `src`"
        )
    }
}
