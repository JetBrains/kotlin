/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.confidence

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class EnableAutopopupInStringTemplate : CompletionConfidence() {
    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        val stringTemplate =
            contextElement.prevLeaf()?.getParentOfType<KtSimpleNameStringTemplateEntry>(strict = false) ?: return ThreeState.UNSURE

        // "$<caret>nameRef" stringTemplate here is "$nameRef", so offset are inside template, we should show lookup
        if (offset in stringTemplate.startOffset until stringTemplate.endOffset) return ThreeState.NO

        val textRange = TextRange.create(stringTemplate.endOffset, offset)
        val containsWhitespaces = textRange.substring(psiFile.text).any { it.isWhitespace() }
        return if (containsWhitespaces) ThreeState.UNSURE else ThreeState.NO
    }
}