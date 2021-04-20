/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.internal

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.uast.UComment
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

interface FirKotlinUElementWithComments : UElement {
    override val comments: List<UComment>
        get() {
            val psi = sourcePsi ?: return emptyList()
            val childrenComments = psi.children.filterIsInstance<PsiComment>().map { UComment(it, this) }
            if (this !is UExpression) return childrenComments
            val childrenAndSiblingComments = childrenComments +
                    psi.nearestCommentSibling(forward = true)?.let { listOf(UComment(it, this)) }.orEmpty() +
                    psi.nearestCommentSibling(forward = false)?.let { listOf(UComment(it, this)) }.orEmpty()
            val parent = psi.parent as? KtValueArgument ?: return childrenAndSiblingComments

            return childrenAndSiblingComments +
                    parent.nearestCommentSibling(forward = true)?.let { listOf(UComment(it, this)) }.orEmpty() +
                    parent.nearestCommentSibling(forward = false)?.let { listOf(UComment(it, this)) }.orEmpty()
        }

    private fun PsiElement.nearestCommentSibling(forward: Boolean): PsiComment? {
        var sibling = if (forward) nextSibling else prevSibling
        while (sibling is PsiWhiteSpace && !sibling.text.contains('\n')) {
            sibling = if (forward) sibling.nextSibling else sibling.prevSibling
        }
        return sibling as? PsiComment
    }
}
