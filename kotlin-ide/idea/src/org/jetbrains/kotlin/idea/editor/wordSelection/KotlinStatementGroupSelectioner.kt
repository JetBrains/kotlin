/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.editor.wordSelection

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.siblings

/**
 * Originally from IDEA platform: StatementGroupSelectioner
 */
class KotlinStatementGroupSelectioner : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement): Boolean {
        if (e !is KtExpression && e !is KtWhenEntry && e !is KtParameterList && e !is PsiComment) return false
        val parent = e.parent
        return parent is KtBlockExpression || parent is KtWhenExpression || parent is KtFunctionLiteral
    }

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        val parent = e.parent

        val startElement = e.siblings(forward = false, withItself = false)
            .firstOrNull {
                // find preceding '{' or blank line
                it is LeafPsiElement && it.elementType == KtTokens.LBRACE || it is PsiWhiteSpace && it.getText()!!.count { it == '\n' } > 1
                        || (it is LeafPsiElement && it.elementType == KtTokens.ARROW && e.getLineNumber() != it.getLineNumber())
            }
            ?.siblings(forward = true, withItself = false)
            ?.dropWhile { it is PsiWhiteSpace } // and take first non-whitespace element after it
            ?.firstOrNull() ?: parent.firstChild!!

        val endElement = e.siblings(forward = true, withItself = false)
            .firstOrNull {
                // find next '}' or blank line
                it is LeafPsiElement && it.elementType == KtTokens.RBRACE || it is PsiWhiteSpace && it.getText()!!.count { it == '\n' } > 1
            }
            ?.siblings(forward = false, withItself = false)
            ?.dropWhile { it is PsiWhiteSpace } // and take first non-whitespace element before it
            ?.firstOrNull() ?: parent.lastChild!!

        return expandToWholeLine(
            editorText,
            TextRange(
                startElement.textRange!!.startOffset,
                endElement.textRange!!.endOffset
            )
        )
    }
}
