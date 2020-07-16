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
import org.jetbrains.kotlin.idea.formatter.KotlinCommonCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.KotlinObsoleteCodeStyle
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/*
 * ASTBlock.node is nullable, this extension was introduced to minimize changes
 */
fun ASTBlock.requireNode() = node ?: error("ASTBlock.getNode() returned null")

/**
 * Can be removed with all usages after moving master to 1.3 with new default code style settings.
 */
val KotlinCodeStyleSettings.isDefaultIntellijOrObsoleteCodeStyle: Boolean get() = CODE_STYLE_DEFAULTS != KotlinStyleGuideCodeStyle.CODE_STYLE_ID
val KotlinCommonCodeStyleSettings.isDefaultIntellijOrObsoleteCodeStyle: Boolean get() = CODE_STYLE_DEFAULTS != KotlinStyleGuideCodeStyle.CODE_STYLE_ID

// Copied from idea-core
fun PsiElement.getLineCount(): Int {
    val doc = containingFile?.let { PsiDocumentManager.getInstance(project).getDocument(it) }
    if (doc != null) {
        val spaceRange = textRange ?: TextRange.EMPTY_RANGE

        if (spaceRange.endOffset <= doc.textLength && spaceRange.startOffset < spaceRange.endOffset) {
            val startLine = doc.getLineNumber(spaceRange.startOffset)
            val endLine = doc.getLineNumber(spaceRange.endOffset)

            return endLine - startLine + 1
        }
    }

    return StringUtil.getLineBreakCount(text ?: error("Cannot count number of lines")) + 1
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

fun PsiElement.containsLineBreakInThis(globalStartOffset: Int, globalEndOffset: Int): Boolean {
    val textRange = TextRange.create(globalStartOffset, globalEndOffset).shiftLeft(startOffset)
    return StringUtil.containsLineBreak(textRange.subSequence(text))
}

fun applyKotlinCodeStyle(codeStyleId: String?, codeStyleSettings: KotlinCodeStyleSettings, modifyCodeStyle: Boolean = true): Boolean {
    when (codeStyleId) {
        KotlinStyleGuideCodeStyle.CODE_STYLE_ID -> KotlinStyleGuideCodeStyle.applyToKotlinCustomSettings(codeStyleSettings, modifyCodeStyle)
        KotlinObsoleteCodeStyle.CODE_STYLE_ID -> KotlinObsoleteCodeStyle.applyToKotlinCustomSettings(codeStyleSettings, modifyCodeStyle)
        else -> return false
    }

    return true
}

fun applyKotlinCodeStyle(codeStyleId: String?, codeStyleSettings: CommonCodeStyleSettings, modifyCodeStyle: Boolean = true): Boolean {
    when (codeStyleId) {
        KotlinStyleGuideCodeStyle.CODE_STYLE_ID -> KotlinStyleGuideCodeStyle.applyToCommonSettings(codeStyleSettings, modifyCodeStyle)
        KotlinObsoleteCodeStyle.CODE_STYLE_ID -> KotlinObsoleteCodeStyle.applyToCommonSettings(codeStyleSettings, modifyCodeStyle)
        else -> return false
    }

    return true
}

fun applyKotlinCodeStyle(codeStyleId: String?, codeStyleSettings: CodeStyleSettings): Boolean {
    when (codeStyleId) {
        KotlinStyleGuideCodeStyle.CODE_STYLE_ID -> KotlinStyleGuideCodeStyle.apply(codeStyleSettings)
        KotlinObsoleteCodeStyle.CODE_STYLE_ID -> KotlinObsoleteCodeStyle.apply(codeStyleSettings)
        else -> return false
    }

    return true
}
