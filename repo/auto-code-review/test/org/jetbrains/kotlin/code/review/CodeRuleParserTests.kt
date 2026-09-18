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
            
            Pattern: *.kt
            
            Pattern: !test
            
            Pattern: !testData
            
            Rule text
        """.trimIndent()

        val rule = parse(text)

        assertEquals(listOf("*.kt", "!test", "!testData"), rule.patterns.patterns)
    }

    @Test
    fun `pattern after exclusion pattern is rejected`() {
        val text = """
            # Rule
            
            Pattern: !test
            
            Pattern: *.kt
            
            Rule text
        """.trimIndent()

        assertRuleIsRejected(
            text,
            "exclusion patterns (!) must go after all other patterns", "*.kt"
        )
    }
}
