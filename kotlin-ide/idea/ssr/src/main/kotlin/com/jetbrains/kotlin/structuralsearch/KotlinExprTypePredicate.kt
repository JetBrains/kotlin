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
import org.jetbrains.kotlinx.serialization.compiler.resolve.toSimpleType

class KotlinExprTypePredicate(
    private val searchedTypeNames: List<String>,
    private val withinHierachy: Boolean,
    private val ignoreCase: Boolean
) : MatchPredicate() {
    override fun match(matchedNode: PsiElement, start: Int, end: Int, context: MatchContext): Boolean {
        val node = StructuralSearchUtil.getParentIfIdentifier(matchedNode)
        return when (node) {
            is KtDeclaration -> node.type()
            is KtExpression -> node.resolveType()
            else -> throw IllegalStateException("Kotlin matching element should either be an expression or a statement.")
        }?.let { resolvedType ->
            val project = node.project
            val scope = project.allScope()
            val subTypes = if(withinHierachy) {
                searchedTypeNames.map { searchName ->
                    KotlinSuperClassIndex.getInstance().get(searchName, project, scope).map {
                        (it.descriptor as ClassDescriptor).toSimpleType()
                    }
                }.flatten().toMutableSet()
            } else mutableSetOf()
            searchedTypeNames.forEach { searchName ->
                subTypes.addAll(
                    if(searchName.contains(".")) {
                        KotlinFullClassNameIndex.getInstance().get(searchName, project, scope).map {
                            (it.descriptor as ClassDescriptor).toSimpleType()
                        }
                    } else {
                        KotlinClassShortNameIndex.getInstance().get(searchName, project, scope).map {
                            (it.descriptor as ClassDescriptor).toSimpleType()
                        }
                    }
                )
            }
            subTypes.any { searchType ->
                "${resolvedType.fqName?.shortName()}".equals("${searchType.fqName?.shortName()}", ignoreCase)
                        || "${resolvedType.fqName}".equals("${searchType.fqName}", ignoreCase)
            }
        } ?: false
    }
}