/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.nullability

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.nj2k.inference.common.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability
import org.jetbrains.kotlin.resolve.jvm.checkers.mustNotBeNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.util.javaslang.getOrNull

class NullabilityBoundTypeEnhancer(private val resolutionFacade: ResolutionFacade) : BoundTypeEnhancer() {
    override fun enhance(
        expression: KtExpression,
        boundType: BoundType,
        inferenceContext: InferenceContext
    ): BoundType {
        return when {
            expression.isNullExpression() ->
                WithForcedStateBoundType(boundType, State.UPPER)
            expression is KtCallExpression -> enhanceCallExpression(expression, boundType, inferenceContext)
            expression is KtQualifiedExpression && expression.selectorExpression is KtCallExpression ->
                enhanceCallExpression(expression.selectorExpression as KtCallExpression, boundType, inferenceContext)
            expression is KtNameReferenceExpression ->
                boundType.enhanceWith(expression.smartCastEnhancement())
            expression is KtLambdaExpression ->
                WithForcedStateBoundType(boundType, State.LOWER)
            else -> boundType
        }
    }

    private fun enhanceCallExpression(
        expression: KtCallExpression,
        boundType: BoundType,
        inferenceContext: InferenceContext
    ): BoundType {
        if (expression.resolveToCall(resolutionFacade)?.candidateDescriptor is ConstructorDescriptor) {
            return WithForcedStateBoundType(boundType, State.LOWER)
        }

        val resolved = expression.calleeExpression?.mainReference?.resolve() ?: return boundType
        if (inferenceContext.isInConversionScope(resolved)) return boundType
        return boundType.enhanceWith(expression.getExternallyAnnotatedForcedState())
    }

    override fun enhanceKotlinType(
        type: KotlinType,
        boundType: BoundType,
        allowLowerEnhancement: Boolean,
        inferenceContext: InferenceContext
    ): BoundType {
        if (type.arguments.size != boundType.typeParameters.size) return boundType
        val inner = BoundTypeImpl(
            boundType.label,
            boundType.typeParameters.zip(type.arguments) { typeParameter, typeArgument ->
                TypeParameter(
                    enhanceKotlinType(
                        typeArgument.type,
                        typeParameter.boundType,
                        allowLowerEnhancement,
                        inferenceContext
                    ),
                    typeParameter.variance
                )
            }
        )
        val enhancement = when {
            type.isMarkedNullable -> State.UPPER
            allowLowerEnhancement -> State.LOWER
            else -> null
        }
        return inner.enhanceWith(enhancement)
    }

    private fun KtReferenceExpression.smartCastEnhancement() = analyzeExpressionUponTheTypeInfo { dataFlowValue, dataFlowInfo, _ ->
        if (dataFlowInfo.completeNullabilityInfo.get(dataFlowValue)?.getOrNull() == Nullability.NOT_NULL) State.LOWER
        else null
    }

    private inline fun KtExpression.analyzeExpressionUponTheTypeInfo(analyzer: (DataFlowValue, DataFlowInfo, KotlinType) -> State?): State? {
        val bindingContext = analyze(resolutionFacade)
        val type = getType(bindingContext) ?: return null

        val dataFlowValue = resolutionFacade.frontendService<DataFlowValueFactory>()
            .createDataFlowValue(this, type, bindingContext, resolutionFacade.moduleDescriptor)
        val dataFlowInfo = bindingContext[BindingContext.EXPRESSION_TYPE_INFO, this]?.dataFlowInfo ?: return null
        return analyzer(dataFlowValue, dataFlowInfo, type)
    }

    private fun KtExpression.getExternallyAnnotatedForcedState() = analyzeExpressionUponTheTypeInfo { dataFlowValue, dataFlowInfo, type ->
        if (!type.isNullable()) return@analyzeExpressionUponTheTypeInfo State.LOWER
        when {
            dataFlowInfo.completeNullabilityInfo.get(dataFlowValue)?.getOrNull() == Nullability.NOT_NULL -> State.LOWER
            type.isExternallyAnnotatedNotNull(dataFlowInfo, dataFlowValue) -> State.LOWER
            else -> null
        }
    }

    private fun KotlinType.isExternallyAnnotatedNotNull(dataFlowInfo: DataFlowInfo, dataFlowValue: DataFlowValue): Boolean =
        mustNotBeNull()?.isFromJava == true && dataFlowInfo.getStableNullability(dataFlowValue).canBeNull()
}