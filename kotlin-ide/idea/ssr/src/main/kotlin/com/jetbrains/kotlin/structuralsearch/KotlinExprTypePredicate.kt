package com.jetbrains.kotlin.structuralsearch

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.predicates.MatchPredicate
import com.intellij.structuralsearch.impl.matcher.predicates.RegExpPredicate
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlinx.serialization.compiler.resolve.toSimpleType

class KotlinExprTypePredicate(
    private val search: String,
    private val withinHierarchy: Boolean,
    private val ignoreCase: Boolean,
    private val target: Boolean,
    private val baseName: String,
    private val regex: Boolean
) : MatchPredicate() {

    override fun match(matchedNode: PsiElement, start: Int, end: Int, context: MatchContext): Boolean {
        val searchedTypeNames = if (regex) listOf() else search.split('|')
        if (matchedNode is KtExpression && matchedNode.isNull() && searchedTypeNames.contains("null")) return true
        val node = StructuralSearchUtil.getParentIfIdentifier(matchedNode)
        val type = when {
            node is KtDeclaration -> node.type()
            node is KtExpression -> try {
                node.resolveType()
            } catch (e: Exception) {
                null
            }
            node is KtStringTemplateEntry && node !is KtSimpleNameStringTemplateEntry -> null
            node is KtSimpleNameStringTemplateEntry -> node.expression?.resolveType()
            else -> throw IllegalStateException(KSSRBundle.message("error.type.filter.node"))
        } ?: return false

        val project = node.project
        val scope = project.allScope()

        if (regex) {
            val delegate = RegExpPredicate(search, !ignoreCase, baseName, false, target)
            return delegate.doMatch(type.fqName.toString(), context, matchedNode)
        }

        for (searchedType in searchedTypeNames) {
            if (searchedType == "null") continue
            val searchNameCorrected = searchedType.substringBefore("<").substringBefore("?")

            val index =
                if (searchedType.contains(".")) KotlinFullClassNameIndex.getInstance()
                else KotlinClassShortNameIndex.getInstance()

            index.get(searchNameCorrected, project, scope).firstOrNull()?.let {
                val searchedKotlinClass = (it.descriptor as ClassDescriptor).toSimpleType('?' in searchedType)
                if (searchedKotlinClass.fqName == type.fqName && searchedKotlinClass.isMarkedNullable == type.isMarkedNullable
                    || withinHierarchy && type.isSubtypeOf(searchedKotlinClass)
                ) return@match true
            }
        }
        return false
    }

}