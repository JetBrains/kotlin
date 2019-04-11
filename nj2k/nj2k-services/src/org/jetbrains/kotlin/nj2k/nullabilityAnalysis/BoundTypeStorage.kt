/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import com.intellij.psi.PsiComment
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class BoundTypeStorage(private val analysisAnalysisContext: AnalysisContext, private val printConstraints: Boolean) {
    private val cache = mutableMapOf<KtExpression, BoundType>()
    private val printer = Printer(analysisAnalysisContext)

    fun boundTypeFor(expression: KtExpression): BoundType =
        cache.getOrPut(expression) {
            if (expression is KtParenthesizedExpression) return@getOrPut boundTypeFor(expression.expression!!)
            val boundType =
                when (expression) {
                    is KtParenthesizedExpression -> expression.expression?.let { boundTypeFor(it) }
                    is KtQualifiedExpression ->
                        expression.selectorExpression?.toBoundType(
                            boundTypeFor(expression.receiverExpression)
                        )
                    else -> expression.getQualifiedExpressionForSelector()?.let { boundTypeFor(it) }
                } ?: expression.toBoundType(null)
                ?: LiteralBoundType(expression.isNullable())


            if (printConstraints) {
                if (expression.getNextSiblingIgnoringWhitespace() !is PsiComment) {
                    val comment = with(printer) {
                        KtPsiFactory(expression.project).createComment("/*${boundType.asString()}*/")
                    }
                    expression.parent.addAfter(comment, expression)
                }
            }
            boundType
        }

    fun boundTypeForType(
        type: KotlinType,
        contextBoundType: BoundType?,
        typeParameterDescriptors: Map<TypeParameterDescriptor, TypeVariable>
    ): BoundType? =
        type.toBoundType(contextBoundType, typeParameterDescriptors)

    private fun KtExpression.resolveToTypeVariable(): TypeVariable? =
        getCalleeExpressionIfAny()
            ?.safeAs<KtReferenceExpression>()
            ?.resolve()
            ?.safeAs<KtDeclaration>()
            ?.let {
                analysisAnalysisContext.declarationToTypeVariable[it]
            } ?: KtPsiUtil.deparenthesize(this)?.let { analysisAnalysisContext.declarationToTypeVariable[it] }

    private fun KtExpression.toBoundTypeAsTypeVariable(): BoundType? =
        resolveToTypeVariable()?.let { TypeVariableBoundType(it, getForcedNullability()) }

    private fun KtExpression.toBoundTypeAsCallExpression(contextBoundType: BoundType?): BoundType? {
        val typeElement = getCalleeExpressionIfAny()
            ?.safeAs<KtReferenceExpression>()
            ?.resolve()
            ?.safeAs<KtCallableDeclaration>()
            ?.typeReference
            ?.typeElement
        typeElement?.let { analysisAnalysisContext.typeElementToTypeVariable[it] }?.also {
            return TypeVariableBoundType(it)
        }
        val bindingContext = analyze()
        val descriptor =
            getResolvedCall(bindingContext)?.candidateDescriptor?.original?.safeAs<CallableDescriptor>() ?: return null
        val typeParameters =
            if (this is KtCallElement) {
                typeArguments.mapIndexed { index, typeArgument ->
                    //TODO better check
                    descriptor.typeParameters[index] to
                            analysisAnalysisContext.typeElementToTypeVariable.getValue(typeArgument.typeReference?.typeElement!!)
                }.toMap()
            } else emptyMap()
        return descriptor.returnType?.toBoundType(contextBoundType, typeParameters)
    }

    private fun KtExpression.toBoundTypeAsCastExpression(): BoundType? {
        val castExpression = KtPsiUtil.deparenthesize(this)
            ?.safeAs<KtBinaryExpressionWithTypeRHS>()
            ?.takeIf { KtPsiUtil.isUnsafeCast(it) }
            ?: return null
        return castExpression.right?.typeElement
            ?.let { analysisAnalysisContext.typeElementToTypeVariable[it] }
            ?.let { TypeVariableBoundType(it) }
    }

    private fun KtExpression.toBoundType(contextBoundType: BoundType?): BoundType? {
        toBoundTypeAsTypeVariable()?.also { return it }
        toBoundTypeAsCallExpression(contextBoundType)?.also { return it }
        toBoundTypeAsCastExpression()?.also { return it }
        return null
    }

    private fun KotlinType.toBoundType(
        contextBoundType: BoundType?,
        typeParameterDescriptors: Map<TypeParameterDescriptor, TypeVariable>
    ): BoundType? {
        fun KotlinType.toBoundType(): BoundType? {
            val forcedNullability = when {
                isMarkedNullable -> Nullability.NULLABLE
                else -> null
            }
            val target = constructor.declarationDescriptor
            return when (target) {
                is ClassDescriptor -> {
                    val classReference = DescriptorClassReference(target)
                    GenericBoundType(
                        classReference,
                        (arguments zip constructor.parameters).map { (typeArgument, typeParameter) ->
                            BoundTypeTypeParameter(
                                typeArgument.type.toBoundType() ?: return null,
                                typeParameter.variance
                            )
                        },
                        forcedNullability,
                        isNullable()
                    )

                }
                is TypeParameterDescriptor -> {
                    when {
                        target in typeParameterDescriptors ->
                            TypeVariableBoundType(typeParameterDescriptors.getValue(target), forcedNullability)
                        contextBoundType != null ->
                            contextBoundType.typeParameters.getOrNull(target.index)?.boundType?.withForcedNullability(forcedNullability)
                        else -> null
                    }
                }
                else -> error(toString())
            }
        }
        return toBoundType()
    }
}