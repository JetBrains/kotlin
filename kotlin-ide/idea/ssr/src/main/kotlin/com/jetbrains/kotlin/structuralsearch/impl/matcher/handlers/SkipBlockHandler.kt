package com.jetbrains.kotlin.structuralsearch.impl.matcher.handlers

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.handlers.MatchingHandler
import org.jetbrains.kotlin.psi.KtBlockExpression

class SkipBlockHandler : MatchingHandler() {

    override fun match(patternNode: PsiElement?, matchedNode: PsiElement?, context: MatchContext?): Boolean {
        val globalMatchingVisitor = context?.matcher ?: return false
        return when (patternNode) {
            is KtBlockExpression -> globalMatchingVisitor.matchSequentially(patternNode.firstStatement, matchedNode)
            else -> false
        }
    }

}