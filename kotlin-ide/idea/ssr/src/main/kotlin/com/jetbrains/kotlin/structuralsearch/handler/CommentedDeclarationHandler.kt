package com.jetbrains.kotlin.structuralsearch.handler

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.handlers.MatchingHandler
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

class CommentedDeclarationHandler : MatchingHandler() {
    private fun KtDeclaration.getNonKDocCommentChild(): PsiComment? =
        this.getChildrenOfType<PsiComment>().firstOrNull { it !is KDoc }

    override fun match(patternNode: PsiElement?, matchedNode: PsiElement?, context: MatchContext?): Boolean {
        if (context == null) return false
        when (patternNode) {
            is PsiComment -> {
                return when (matchedNode) {
                    // Match PsiComment-s
                    is PsiComment -> context.matcher.match(patternNode, matchedNode)

                    // Match [PsiComment, PROPERTY] with [PROPERTY[PsiComment]]
                    is KtDeclaration -> context.matcher.match(patternNode, matchedNode.getNonKDocCommentChild())
                    else -> false
                }
            }
            is KtDeclaration -> {
                val patternComment = patternNode.getNonKDocCommentChild()

                when {
                    // Comment already matched
                    PsiTreeUtil.skipWhitespacesBackward(patternNode) is PsiComment ->
                        return context.matcher.match(patternNode, matchedNode)

                    // Match [PROPERTY[PsiComment]] with [PROPERTY[PsiComment]]
                    matchedNode is KtDeclaration && patternComment != null ->
                        return context.matcher.match(patternNode, matchedNode)
                                && context.matcher.match(patternComment, matchedNode.getNonKDocCommentChild())

                    // Match [PROPERTY[]] with [PROPERTY[PsiComment?]]
                    matchedNode is KtDeclaration ->
                        return context.matcher.match(patternNode, matchedNode)

                    // Match [PROPERTY[PsiComment]] with [PsiComment, PROPERTY]
                    matchedNode is PsiComment && PsiTreeUtil.skipWhitespacesForward(matchedNode) is KtDeclaration -> {
                        val firstMatch = patternComment != null && context.matcher.match(patternComment, matchedNode)
                        val secondMatch = context.matcher.match(patternNode, PsiTreeUtil.skipWhitespacesForward(matchedNode))
                        if (firstMatch && secondMatch) {
                            context.addMatchedNode(PsiTreeUtil.skipWhitespacesForward(matchedNode))
                            return true
                        }
                    }

                    else -> return false
                }
            }
        }
        return false
    }

    override fun shouldAdvanceTheMatchFor(patternElement: PsiElement?, matchedElement: PsiElement?): Boolean {
        if (patternElement is PsiComment && matchedElement is KtDeclaration) return false
        return super.shouldAdvanceTheMatchFor(patternElement, matchedElement)
    }

}