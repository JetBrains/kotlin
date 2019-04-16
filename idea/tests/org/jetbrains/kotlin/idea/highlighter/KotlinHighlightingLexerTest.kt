/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.testFramework.LexerTestCase
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class KotlinHighlightingLexerTest : LexerTestCase() {
    override fun createLexer() = KotlinHighlightingLexer()

    fun testCharLiteralValidEscape() {
        doTest("'\\n'",  """CHARACTER_LITERAL (''')
                           |VALID_STRING_ESCAPE_TOKEN ('\n')
                           |CHARACTER_LITERAL (''')""".trimMargin())
    }

    fun testCharLiteralInvalidEscape() {
        doTest("'\\q'",  """CHARACTER_LITERAL (''')
                           |INVALID_CHARACTER_ESCAPE_TOKEN ('\q')
                           |CHARACTER_LITERAL (''')""".trimMargin())
    }

    override fun getDirPath() = throw UnsupportedOperationException()
}
