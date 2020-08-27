/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.formatting.ASTBlock
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.KotlinObsoleteCodeStyle
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.*

/*
 * ASTBlock.node is nullable, this extension was introduced to minimize changes
 */
fun ASTBlock.requireNode() = node ?: error("ASTBlock.getNode() returned null")

// Copied from idea-core
fun PsiElement.getLineCount(): Int {
    val spaceRange = textRange ?: TextRange.EMPTY_RANGE
    return getLineCountByDocument(spaceRange.startOffset, spaceRange.endOffset)
        ?: StringUtil.getLineBreakCount(text ?: error("Cannot count number of lines")) + 1
}

fun PsiElement.getLineCountByDocument(startOffset: Int, endOffset: Int): Int? {
    val doc = containingFile?.let { PsiDocumentManager.getInstance(project).getDocument(it) } ?: return null
    if (endOffset > doc.textLength || startOffset >= endOffset) return null

    val startLine = doc.getLineNumber(startOffset)
    val endLine = doc.getLineNumber(endOffset)

    return endLine - startLine + 1
}

fun PsiElement.isMultiline() = getLineCount() > 1

fun PsiElement?.isLineBreak() = this is PsiWhiteSpace && StringUtil.containsLineBreak(text)

fun PsiElement.leafIgnoringWhitespace(forward: Boolean = true, skipEmptyElements: Boolean = true) =
    leaf(forward) { (!skipEmptyElements || it.textLength != 0) && it !is PsiWhiteSpace }

fun PsiElement.leafIgnoringWhitespaceAndComments(forward: Boolean = true, skipEmptyElements: Boolean = true) =
    leaf(forward) { (!skipEmptyElements || it.textLength != 0) && it !is PsiWhiteSpace && it !is PsiComment }

fun PsiElement.leaf(forward: Boolean = true, filter: (PsiElement) -> Boolean): PsiElement? =
    if (forward) nextLeaf(filter)
    else prevLeaf(filter)

val PsiElement.isComma: Boolean get() = PsiUtil.getElementType(this) == KtTokens.COMMA

fun PsiElement.containsLineBreakInChild(globalStartOffset: Int, globalEndOffset: Int): Boolean =
    getLineCountByDocument(globalStartOffset, globalEndOffset)?.let { it > 1 }
        ?: firstChild.siblings(forward = true, withItself = true)
            .dropWhile { it.startOffset < globalStartOffset }
            .takeWhile { it.endOffset <= globalEndOffset }
            .any { it.textContains('\n') || it.textContains('\r') }

fun applyKotlinCodeStyle(
    codeStyleId: String?,
    codeStyleSettings: KotlinCodeStyleSettings,
    modifyCodeStyle: Boolean = true
) = when (codeStyleId) {
    KotlinStyleGuideCodeStyle.CODE_STYLE_ID -> KotlinStyleGuideCodeStyle.applyToKotlinCustomSettings(codeStyleSettings, modifyCodeStyle)
    KotlinObsoleteCodeStyle.CODE_STYLE_ID -> KotlinObsoleteCodeStyle.applyToKotlinCustomSettings(codeStyleSettings, modifyCodeStyle)
    else -> Unit
}

fun applyKotlinCodeStyle(
    codeStyleId: String?,
    codeStyleSettings: CommonCodeStyleSettings,
    modifyCodeStyle: Boolean = true
) = when (codeStyleId) {
    KotlinStyleGuideCodeStyle.CODE_STYLE_ID -> KotlinStyleGuideCodeStyle.applyToCommonSettings(codeStyleSettings, modifyCodeStyle)
    KotlinObsoleteCodeStyle.CODE_STYLE_ID -> KotlinObsoleteCodeStyle.applyToCommonSettings(codeStyleSettings, modifyCodeStyle)
    else -> Unit
}

fun applyKotlinCodeStyle(codeStyleId: String?, codeStyleSettings: CodeStyleSettings): Boolean {
    when (codeStyleId) {
        KotlinStyleGuideCodeStyle.CODE_STYLE_ID -> KotlinStyleGuideCodeStyle.apply(codeStyleSettings)
        KotlinObsoleteCodeStyle.CODE_STYLE_ID -> KotlinObsoleteCodeStyle.apply(codeStyleSettings)
        else -> return false
    }

    return true
}
