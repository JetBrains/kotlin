package com.jetbrains.kotlin.structuralsearch.impl.matcher.strategies

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.impl.matcher.strategies.MatchingStrategy
import org.jetbrains.kotlin.psi.KtElement

object KotlinMatchingStrategy : MatchingStrategy {
    override fun continueMatching(start: PsiElement?): Boolean {
        return start is KtElement
    }

    override fun shouldSkip(element: PsiElement?, elementToMatchWith: PsiElement?): Boolean {
        return false
    }
}