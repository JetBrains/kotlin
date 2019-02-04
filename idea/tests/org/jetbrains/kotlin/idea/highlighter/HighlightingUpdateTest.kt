/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
