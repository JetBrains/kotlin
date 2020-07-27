/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInliner

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.containers.stopAfter
import org.jetbrains.kotlin.idea.util.isLineBreak
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

class CommentHolder(val leadingComments: List<CommentNode>, val trailingComments: List<CommentNode>) {
    fun restoreComments(element: PsiElement) {
        val factory = KtPsiFactory(element)
        for (leadingComment in leadingComments) {
            addComment(factory, leadingComment, element, true)
        }

        for (trailingComment in trailingComments.asReversed()) {
            addComment(factory, trailingComment, element, false)
        }
    }

    fun merge(other: CommentHolder): CommentHolder = CommentHolder(
        other.leadingComments + this.leadingComments,
        this.trailingComments + other.trailingComments
    )

    val isEmpty: Boolean get() = leadingComments.isEmpty() && trailingComments.isEmpty()

    class CommentNode(val indentBefore: String, val comment: String, val indentAfter: String) {
        companion object {
            fun create(comment: PsiComment): CommentNode = CommentNode(indentBefore(comment), comment.text, indentAfter(comment))

            fun PsiElement.mergeComments(commentHolder: CommentHolder) {
                val oldHolder = getCopyableUserData(COMMENTS_TO_RESTORE_KEY)
                val newCommentHolder = oldHolder?.merge(commentHolder) ?: commentHolder

                putCopyableUserData(COMMENTS_TO_RESTORE_KEY, newCommentHolder)
            }
        }
    }

    companion object {
        val COMMENTS_TO_RESTORE_KEY: Key<CommentHolder> = Key("COMMENTS_TO_RESTORE")

        fun extract(blockExpression: KtBlockExpression): Sequence<CommentHolder> = sequence {
            val children = blockExpression.children().mapNotNull { it.psi }.iterator()

            while (children.hasNext()) {
                val before = children.stopAfter { it is KtExpression }.asSequence().collectComments()
                val after = children.stopAfter { it.isLineBreak() || it is KtExpression }.asSequence().collectComments()

                yield(CommentHolder(before, after))
            }
        }

        fun Sequence<PsiElement>.collectComments(): List<CommentNode> = this.filterIsInstance<PsiComment>()
            .map { CommentNode.create(it) }
            .toList()
    }
}

private fun indentBefore(comment: PsiComment): String {
    val prevWhiteSpace = comment.prevLeaf() as? PsiWhiteSpace ?: return ""
    val whiteSpaceText = prevWhiteSpace.text

    if (prevWhiteSpace.prevSibling is PsiComment) return whiteSpaceText

    val indexOfLineBreak = whiteSpaceText.indexOfLast { StringUtil.isLineBreak(it) }
    if (indexOfLineBreak == -1) return whiteSpaceText

    val startIndex = indexOfLineBreak + 1

    if (startIndex >= whiteSpaceText.length) return ""
    return whiteSpaceText.substring(startIndex)
}

private fun indentAfter(comment: PsiComment): String {
    val nextWhiteSpace = comment.nextLeaf() as? PsiWhiteSpace ?: return ""
    if (nextWhiteSpace.nextSibling is PsiComment) return ""

    val whiteSpaceText = nextWhiteSpace.text
    val indexOfLineBreak = whiteSpaceText.indexOfFirst { StringUtil.isLineBreak(it) }
    if (indexOfLineBreak == -1) return whiteSpaceText

    val endIndex = indexOfLineBreak + 1
    if (endIndex > whiteSpaceText.length) return whiteSpaceText
    return whiteSpaceText.substring(0, endIndex)
}

private fun addSiblingFunction(before: Boolean): (PsiElement, PsiElement, PsiElement) -> PsiElement = if (before)
    PsiElement::addBefore
else
    PsiElement::addAfter

private fun PsiElement.addWhiteSpace(factory: KtPsiFactory, whiteSpaceText: String, before: Boolean) {
    if (whiteSpaceText.isEmpty()) return
    val siblingWhiteSpace = (if (before) prevLeaf() else nextLeaf()) as? PsiWhiteSpace
    if (siblingWhiteSpace == null) {
        addSiblingFunction(before)(parent, factory.createWhiteSpace(whiteSpaceText), this)
    }
    else {
        val siblingText = siblingWhiteSpace.text
        val containsLineBreak = StringUtil.containsLineBreak(siblingText) && StringUtil.containsLineBreak(whiteSpaceText)

        val newWhiteSpaceText = if (before) {
            if (containsLineBreak) whiteSpaceText
            else siblingText + whiteSpaceText
        }
        else {
            if (containsLineBreak) return
            whiteSpaceText + siblingText
        }

        siblingWhiteSpace.replace(factory.createWhiteSpace(newWhiteSpaceText))
    }
}

private fun addComment(factory: KtPsiFactory, commentNode: CommentHolder.CommentNode, target: PsiElement, before: Boolean) {
    val parent = target.parent
    val comment = factory.createComment(commentNode.comment)
    addSiblingFunction(before && parent !is KtReturnExpression)(parent, comment, target).updateWhiteSpaces(factory, commentNode)
}

private fun PsiElement.updateWhiteSpaces(factory: KtPsiFactory, commentNode: CommentHolder.CommentNode) {
    addWhiteSpace(factory, commentNode.indentBefore, before = true)
    addWhiteSpace(factory, commentNode.indentAfter, before = false)
}