/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.lexer.KtTokens
import kotlin.test.assertEquals

class HighlightingUpdateTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinLightProjectDescriptor.INSTANCE

    fun testUpdateWithBackticks() {
        val editorText = "fun `two words`() { }"
        myFixture.configureByText("foo.kt", editorText)
        val editorHighlighter = (myFixture.editor as EditorEx).highlighter as LexerEditorHighlighter

        val index = editorText.indexOf("`()")
        myFixture.project.executeWriteCommand("") {
            editorHighlighter.assertHighlighterIteratorTokens(KtTokens.FUN_KEYWORD, KtTokens.WHITE_SPACE, KtTokens.IDENTIFIER)

            myFixture.editor.document.deleteString(index, index + 1)

            editorHighlighter.assertHighlighterIteratorTokens(KtTokens.FUN_KEYWORD, KtTokens.WHITE_SPACE, TokenType.BAD_CHARACTER)

            myFixture.editor.document.insertString(index, "`")

            editorHighlighter.assertHighlighterIteratorTokens(KtTokens.FUN_KEYWORD, KtTokens.WHITE_SPACE, KtTokens.IDENTIFIER)
        }
    }
}

private fun LexerEditorHighlighter.assertHighlighterIteratorTokens(vararg tokens: IElementType) {
    val iterator = createIterator(0)
    for (token in tokens) {
        assertEquals(token, iterator.tokenType)
        iterator.advance()
    }
}
