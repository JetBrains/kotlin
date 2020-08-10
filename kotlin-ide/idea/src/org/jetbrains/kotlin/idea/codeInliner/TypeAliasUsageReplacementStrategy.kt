/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInliner

import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance

class TypeAliasUsageReplacementStrategy(val typeAlias: KtTypeAlias) : UsageReplacementStrategy {
    override fun createReplacer(usage: KtReferenceExpression): (() -> KtElement?)? {
        val refElement = usage.getParentOfTypeAndBranch<KtUserType> { referenceExpression }
            ?: usage.getNonStrictParentOfType<KtSimpleNameExpression>() ?: return null

        val typeAliasDescriptor = typeAlias.unsafeResolveToDescriptor() as TypeAliasDescriptor
        val typeToInline = typeAliasDescriptor.expandedType
        val typeConstructorsToInline = typeAliasDescriptor.typeConstructor.parameters.map { it.typeConstructor }
        val psiFactory = KtPsiFactory(typeAlias)

        fun inlineIntoType(usage: KtUserType): KtElement? {
            val context = usage.analyze(BodyResolveMode.PARTIAL)
            val argumentTypes = usage
                .typeArguments
                .asSequence()
                .filterNotNull()
                .mapNotNull {
                    val type =
                        context[BindingContext.ABBREVIATED_TYPE, it.typeReference] ?: context[BindingContext.TYPE, it.typeReference]
                    if (type != null) TypeProjectionImpl(type) else null
                }
                .toList()

            if (argumentTypes.size != typeConstructorsToInline.size) return null
            val substitution = (typeConstructorsToInline zip argumentTypes).toMap()
            val substitutor = TypeSubstitutor.create(substitution)
            val expandedType = substitutor.substitute(typeToInline, Variance.INVARIANT) ?: return null
            val expandedTypeText = IdeDescriptorRenderers.SOURCE_CODE.renderType(expandedType)
            val needParentheses =
                (expandedType.isFunctionType && usage.parent is KtNullableType) || (expandedType.isExtensionFunctionType && usage.getParentOfTypeAndBranch<KtFunctionType> { receiverTypeReference } != null)
            val expandedTypeReference = psiFactory.createType(expandedTypeText)
            return usage.replaced(expandedTypeReference.typeElement!!).apply {
                if (needParentheses) {
                    val sample = psiFactory.createParameterList("()")
                    parent.addBefore(sample.firstChild, this)
                    parent.addAfter(sample.lastChild, this)
                }
            }
        }

        fun inlineIntoCall(usage: KtReferenceExpression): KtElement? {
            val importDirective = usage.getStrictParentOfType<KtImportDirective>()
            if (importDirective != null) {
                val reference = usage.getQualifiedElementSelector()?.mainReference
                if (reference != null && reference.multiResolve(false).size <= 1) {
                    importDirective.delete()
                }

                return null
            }

            val resolvedCall = usage.resolveToCall() ?: return null
            val callElement = resolvedCall.call.callElement as? KtCallElement ?: return null
            val substitution = resolvedCall.typeArguments
                .mapKeys { it.key.typeConstructor }
                .mapValues { TypeProjectionImpl(it.value) }
            if (substitution.size != typeConstructorsToInline.size) return null
            val substitutor = TypeSubstitutor.create(substitution)
            val expandedType = substitutor.substitute(typeToInline, Variance.INVARIANT) ?: return null
            val expandedTypeFqName = expandedType.constructor.declarationDescriptor?.importableFqName ?: return null

            if (expandedType.arguments.isNotEmpty()) {
                val expandedTypeArgumentList = psiFactory.createTypeArguments(
                    expandedType.arguments.joinToString(
                        prefix = "<",
                        postfix = ">"
                    ) { IdeDescriptorRenderers.SOURCE_CODE.renderType(it.type) }
                )

                val originalTypeArgumentList = callElement.typeArgumentList
                if (originalTypeArgumentList != null) {
                    originalTypeArgumentList.replaced(expandedTypeArgumentList)
                } else {
                    callElement.addAfter(expandedTypeArgumentList, callElement.calleeExpression)
                }
            }

            val newCallElement = ((usage.mainReference as KtSimpleNameReference).bindToFqName(
                expandedTypeFqName,
                KtSimpleNameReference.ShorteningMode.NO_SHORTENING
            ) as KtExpression).getNonStrictParentOfType<KtCallElement>()
            return newCallElement?.getQualifiedExpressionForSelector() ?: newCallElement
        }

        return when (refElement) {
            is KtUserType -> {
                { inlineIntoType(refElement)?.also { it.addToShorteningWaitSet() } }
            }
            else -> {
                { inlineIntoCall(refElement as KtReferenceExpression)?.also { it.addToShorteningWaitSet() } }
            }
        }
    }

}