package com.jetbrains.kotlin.structuralsearch

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.predicates.MatchPredicate
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty

class KotlinExprTypePredicate(
    private val searchedTypeName: String,
    private val withinHierachy: Boolean,
    private val ignoreCase: Boolean
) : MatchPredicate() {
    override fun match(matchedNode: PsiElement, start: Int, end: Int, context: MatchContext): Boolean {
        val resolvedType = when (val node = StructuralSearchUtil.getParentIfIdentifier(matchedNode)) {
            is KtDeclaration -> node.type()
            is KtExpression -> node.resolveType()
            else -> throw IllegalStateException(
                "Kotlin matching element should either be an expression or a statement."
            )
        }
        return searchedTypeName.equals(resolvedType?.fqName.toString(), ignoreCase)
                || searchedTypeName.equals("$resolvedType", ignoreCase)
    }
}