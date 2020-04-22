package com.jetbrains.kotlin.structuralsearch.impl.matcher.handlers

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.handlers.MatchingHandler
import org.jetbrains.kotlin.psi.KtBlockExpression

class SkipBlockHandler : MatchingHandler() {

    override fun match(patternNode: PsiElement?, matchedNode: PsiElement?, context: MatchContext?): Boolean {
        return when (patternNode) {
            is KtBlockExpression -> context?.matcher?.match(patternNode.firstStatement, matchedNode) ?: false
            else -> context?.matcher?.match(patternNode, matchedNode) ?: false
        }
    }

}