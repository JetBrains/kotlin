package com.jetbrains.kotlin.structuralsearch.impl.matcher.strategies

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.impl.matcher.strategies.MatchingStrategy
import org.jetbrains.kotlin.idea.KotlinLanguage

object KotlinMatchingStrategy : MatchingStrategy {
    override fun continueMatching(start: PsiElement?): Boolean {
        return start?.language == KotlinLanguage.INSTANCE
    }

    override fun shouldSkip(element: PsiElement?, elementToMatchWith: PsiElement?): Boolean {
        return false
    }
}