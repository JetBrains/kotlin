package com.jetbrains.kotlin.structuralsearch

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.elementType
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.predicates.MatchPredicate
import com.intellij.structuralsearch.impl.matcher.predicates.RegExpPredicate
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.supertypes
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
            val typesToTest = mutableListOf(type)
            if (withinHierarchy) typesToTest.addAll(type.supertypes())

            return typesToTest.any {
                delegate.doMatch(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it), context, matchedNode)
                        || delegate.doMatch(DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(it), context, matchedNode)
            }
        }

        val factory = KtPsiFactory(project, false)
        return searchedTypeNames
            .filter { it != "null" }
            .map(factory::createType)
            .any { typeReference ->
                matchTypeReference(type, typeReference, project, scope)
                        || withinHierarchy
                        && type.supertypes().any { superType -> matchTypeReference(superType, typeReference, project, scope) }
            }
    }

    companion object {
        private fun matchTypeReference(
            type: KotlinType?,
            typeReference: KtTypeReference?,
            project: Project,
            scope: GlobalSearchScope
        ): Boolean {
            if (type == null || typeReference == null) return type == null && typeReference == null
            val element = typeReference.typeElement ?: return false
            return matchTypeElement(type, element, project, scope)
        }

        private fun matchTypeElement(
            type: KotlinType?,
            typeElement: KtTypeElement?,
            project: Project,
            scope: GlobalSearchScope
        ): Boolean {
            if (type == null || typeElement == null) return type == null && typeElement == null

            val matchArguments = typeElement.typeArgumentsAsTypes.isEmpty() ||
                    type.arguments.size == typeElement.typeArgumentsAsTypes.size
                    && type.arguments.zip(typeElement.typeArgumentsAsTypes).all { (projection, reference) ->
                compareProjections(projection, reference) && matchTypeReference(
                    projection.type,
                    reference,
                    project,
                    scope
                )
            }

            val matchSpecific = when (typeElement) {
                is KtFunctionType ->
                    matchNames(type, typeElement)
                            && matchTypeReference(
                        type.getReceiverTypeFromFunctionType(),
                        typeElement.receiverTypeReference,
                        project,
                        scope
                    ) && !type.isMarkedNullable
                is KtUserType -> {
                    val className = typeElement.referencedName ?: return false
                    val index = if (className.contains(".")) KotlinFullClassNameIndex.getInstance()
                    else KotlinClassShortNameIndex.getInstance()
                    index.get(className, project, scope).any {
                        val searchedType = (it.descriptor as ClassDescriptor).toSimpleType(typeElement.parent is KtNullableType)
                        searchedType.fqName == type.fqName
                    } && !type.isMarkedNullable
                }
                is KtNullableType -> type.isMarkedNullable && matchTypeElement(
                    type.makeNotNullable(),
                    typeElement.innerType,
                    project,
                    scope
                )
                else -> throw Error("Malformed type: $typeElement")
            }

            return matchArguments
                    && matchSpecific
        }

        private fun matchNames(type: KotlinType, typeElement: KtTypeElement): Boolean {
            val parent = typeElement.parent
            val typeArguments = typeElement.typeArgumentsAsTypes
            return when {
                parent is KtTypeReference &&
                        parent.modifierList?.allChildren?.any { it.elementType == KtTokens.SUSPEND_KEYWORD } == true
                -> "${type.fqName}" == "kotlin.coroutines.SuspendFunction${typeArguments.size - 1}"
                else -> "${type.fqName}" == "kotlin.Function${typeArguments.size - 1}"
                        || typeArguments.size == 1 && "${type.fqName}" == "kotlin.Function"
            }
        }

        private fun compareProjections(projection: TypeProjection, typeReference: KtTypeReference?): Boolean {
            val parent = typeReference?.parent
            if (parent !is KtTypeProjection) return projection.projectionKind == Variance.INVARIANT

            // TODO: Match star projections
            return when (parent.projectionKind) {
                KtProjectionKind.IN -> projection.projectionKind == Variance.IN_VARIANCE
                KtProjectionKind.OUT -> projection.projectionKind == Variance.OUT_VARIANCE
                KtProjectionKind.NONE -> projection.projectionKind == Variance.INVARIANT
                else -> false
            }
        }
    }

}