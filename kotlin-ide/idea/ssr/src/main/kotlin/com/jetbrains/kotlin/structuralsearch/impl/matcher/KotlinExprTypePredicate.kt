package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.predicates.MatchPredicate
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.psi.KtExpression

class KotlinExprTypePredicate(
    private val searchedTypeName: String,
    private val withinHierachy: Boolean,
    private val ignoreCase: Boolean
) : MatchPredicate() {
    override fun match(matchedNode: PsiElement, start: Int, end: Int, context: MatchContext): Boolean {
        return when(val node = StructuralSearchUtil.getParentIfIdentifier(matchedNode)) {
            is KtExpression -> {
                val resolvedType = node.resolveType()
                searchedTypeName.equals(resolvedType.fqName.toString(), ignoreCase)
                        || searchedTypeName.equals("$resolvedType", ignoreCase)
            }
            else -> false
        }
    }
}