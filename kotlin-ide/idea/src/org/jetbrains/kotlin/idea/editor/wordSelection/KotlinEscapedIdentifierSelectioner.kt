/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor.wordSelection

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KotlinEscapedIdentifierSelectioner : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement) = e.node.elementType == KtTokens.IDENTIFIER

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        val text = e.text
        if (!text.startsWith('`') || !text.endsWith('`')) return null
        val start = e.startOffset + 1
        val end = e.endOffset - 1
        if (start >= end) return null
        return listOf(TextRange(start, end))
    }
}
