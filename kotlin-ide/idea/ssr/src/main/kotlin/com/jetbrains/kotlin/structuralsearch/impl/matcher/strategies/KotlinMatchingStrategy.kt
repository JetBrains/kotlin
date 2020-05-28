package com.jetbrains.kotlin.structuralsearch.impl.matcher.strategies

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.structuralsearch.impl.matcher.strategies.MatchingStrategy
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens

object KotlinMatchingStrategy : MatchingStrategy {
    override fun continueMatching(start: PsiElement?): Boolean = start?.language == KotlinLanguage.INSTANCE

    override fun shouldSkip(element: PsiElement?, elementToMatchWith: PsiElement?): Boolean {
        if (element is LeafPsiElement) return element.elementType == KDocTokens.LEADING_ASTERISK
        return false
    }
}