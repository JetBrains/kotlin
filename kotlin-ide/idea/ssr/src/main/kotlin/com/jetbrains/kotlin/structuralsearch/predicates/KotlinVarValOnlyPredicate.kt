package com.jetbrains.kotlin.structuralsearch.predicates

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.predicates.MatchPredicate
import org.jetbrains.kotlin.psi.KtProperty

class KotlinVarValOnlyPredicate(private val isVar: Boolean) : MatchPredicate() {
    override fun match(matchedNode: PsiElement, start: Int, end: Int, context: MatchContext): Boolean {
        val parent = matchedNode.parent
        if (parent !is KtProperty) return false
        return parent.isVar == isVar
    }
}