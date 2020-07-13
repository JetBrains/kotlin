package com.jetbrains.kotlin.structuralsearch

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.predicates.MatchPredicate
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinSuperClassIndex
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlinx.serialization.compiler.resolve.toSimpleType

class KotlinExprTypePredicate(
    private val searchedTypeNames: List<String>,
    private val withinHierarchy: Boolean,
    private val ignoreCase: Boolean
) : MatchPredicate() {
    override fun match(matchedNode: PsiElement, start: Int, end: Int, context: MatchContext): Boolean {
        val node = StructuralSearchUtil.getParentIfIdentifier(matchedNode)
        val type = when (node) {
            is KtDeclaration -> node.type()
            is KtExpression -> node.resolveType()
            else -> throw IllegalStateException("Kotlin matching element should either be an expression or a statement.")
        } ?: return false

        val project = node.project
        val scope = project.allScope()

        for (searchedType in searchedTypeNames) {
            val nullable = '?' in searchedType
            val searchNameCorrected = searchedType.substringBefore("<").substringBefore("?")

            // Consider super types if within hierarchy is set
            val subTypes: MutableSet<SimpleType> = mutableSetOf()
            if (withinHierarchy) {
                KotlinSuperClassIndex.getInstance().get(searchNameCorrected, project, scope).forEach {
                    val classDescriptor = it.descriptor as ClassDescriptor
                    subTypes.add(classDescriptor.toSimpleType())
                    if (nullable) subTypes.add(classDescriptor.toSimpleType(true))
                }
            }

            val index =
                if (searchedType.contains(".")) KotlinFullClassNameIndex.getInstance()
                else KotlinClassShortNameIndex.getInstance()

            index.get(searchNameCorrected, project, scope).forEach {
                val classDescriptor = it.descriptor as ClassDescriptor
                subTypes.add(classDescriptor.toSimpleType(nullable))
                if (nullable && withinHierarchy) subTypes.add(classDescriptor.toSimpleType())
            }

            if (subTypes.any { searchType ->
                    "${type.fqName}".equals("${searchType.fqName}", ignoreCase)
                            && ((nullable && withinHierarchy) || type.isMarkedNullable == searchType.isMarkedNullable)
                }) return true
        }
        return false
    }

}