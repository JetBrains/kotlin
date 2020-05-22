/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessorHelper
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaContext
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaHelper.findInvalidCommas
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaHelper.trailingCommaOrLastElement
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaState
import org.jetbrains.kotlin.idea.formatter.trailingComma.addTrailingCommaIsAllowedFor
import org.jetbrains.kotlin.idea.formatter.trailingComma.addTrailingCommaIsAllowedForThis
import org.jetbrains.kotlin.idea.util.leafIgnoringWhitespace
import org.jetbrains.kotlin.idea.util.leafIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.siblings

class TrailingCommaPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement =
        TrailingCommaPostFormatVisitor(settings).process(source)

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange =
        TrailingCommaPostFormatVisitor(settings).processText(source, rangeToReformat)
}

private class TrailingCommaPostFormatVisitor(val settings: CodeStyleSettings) : TrailingCommaVisitor() {
    private val myPostProcessor = PostFormatProcessorHelper(settings.kotlinCommonSettings)

    override fun process(trailingCommaContext: TrailingCommaContext) {
        val ktElement = trailingCommaContext.ktElement
        if (!ktElement.addTrailingCommaIsAllowedForThis()) return
        processIfInRange(ktElement) {
            processCommaOwner(trailingCommaContext)
        }
    }

    private fun processIfInRange(element: KtElement, block: () -> Unit = {}) {
        if (myPostProcessor.isElementPartlyInRange(element)) {
            block()
        }
    }

    private fun processCommaOwner(trailingCommaContext: TrailingCommaContext) {
        val ktElement = trailingCommaContext.ktElement
        if (!postFormatIsEnable(ktElement)) return

        val lastElementOrComma = trailingCommaOrLastElement(ktElement) ?: return
        updatePsi(ktElement) {
            val state = trailingCommaContext.state
            when {
                state == TrailingCommaState.MISSING && settings.kotlinCustomSettings.addTrailingCommaIsAllowedFor(ktElement) -> {
                    // add a missing comma
                    if (trailingCommaAllowedInModule(ktElement)) {
                        lastElementOrComma.addCommaAfter(KtPsiFactory(ktElement))
                    }

                    correctCommaPosition(ktElement)
                }
                state == TrailingCommaState.EXISTS -> {
                    correctCommaPosition(ktElement)
                }
                state == TrailingCommaState.REDUNDANT -> {
                    // remove redundant comma
                    lastElementOrComma.delete()
                }
                else -> Unit
            }
        }
    }

    private fun updatePsi(element: KtElement, block: () -> Unit) {
        element.putUserData(IS_INSIDE, true)
        val oldLength = element.parent.textLength
        block()

        val resultElement = CodeStyleManager.getInstance(element.project).reformat(element)
        myPostProcessor.updateResultRange(oldLength, resultElement.parent.textLength)
        element.putUserData(IS_INSIDE, false)
    }

    private fun correctCommaPosition(parent: KtElement) {
        for (pointerToComma in findInvalidCommas(parent).map { it.createSmartPointer() }) {
            pointerToComma.element?.let {
                correctComma(it)
            }
        }
    }

    fun process(formatted: PsiElement): PsiElement {
        LOG.assertTrue(formatted.isValid)
        formatted.accept(this)
        return formatted
    }

    fun processText(
        source: PsiFile,
        rangeToReformat: TextRange,
    ): TextRange {
        myPostProcessor.resultTextRange = rangeToReformat
        source.accept(this)
        return myPostProcessor.resultTextRange
    }

    companion object {
        private val LOG = Logger.getInstance(TrailingCommaVisitor::class.java)
        private val IS_INSIDE = Key.create<Boolean>("TrailingCommaPostFormat")
        private fun postFormatIsEnable(source: PsiElement): Boolean = source.getUserData(IS_INSIDE) != true
    }
}

private fun PsiElement.addCommaAfter(factory: KtPsiFactory) {
    val comma = factory.createComma()
    parent.addAfter(comma, this)
}

private fun correctComma(comma: PsiElement) {
    val prevWithComment = comma.leafIgnoringWhitespace(false) ?: return
    val prevWithoutComment = comma.leafIgnoringWhitespaceAndComments(false) ?: return
    if (prevWithComment != prevWithoutComment) {
        val check = { element: PsiElement -> element is PsiWhiteSpace || element is PsiComment }
        val firstElement = prevWithComment.siblings(forward = false, withItself = true).takeWhile(check).last()
        val commentOwner = prevWithComment.parent
        comma.parent.addRangeAfter(firstElement, prevWithComment, comma)
        commentOwner.deleteChildRange(firstElement, prevWithComment)
    }
}
