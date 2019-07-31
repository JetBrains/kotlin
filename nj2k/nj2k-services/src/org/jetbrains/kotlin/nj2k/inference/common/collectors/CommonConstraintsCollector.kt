/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common.collectors

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.nj2k.inference.common.BoundTypeCalculator
import org.jetbrains.kotlin.nj2k.inference.common.ConstraintBuilder
import org.jetbrains.kotlin.nj2k.inference.common.ConstraintPriority
import org.jetbrains.kotlin.nj2k.inference.common.InferenceContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunction
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class CommonConstraintsCollector : ConstraintsCollector() {
    override fun ConstraintBuilder.collectConstraints(
        element: KtElement,
        boundTypeCalculator: BoundTypeCalculator,
        inferenceContext: InferenceContext,
        resolutionFacade: ResolutionFacade
    ) = with(boundTypeCalculator) {
        when {
            element is KtBinaryExpressionWithTypeRHS && KtPsiUtil.isUnsafeCast(element) -> {
                element.right?.typeElement?.let { inferenceContext.typeElementToTypeVariable[it] }?.also { typeVariable ->
                    element.left.isSubtypeOf(typeVariable, ConstraintPriority.ASSIGNMENT)
                }
            }

            element is KtBinaryExpression && element.asAssignment() != null -> {
                element.right?.isSubtypeOf(element.left ?: return, ConstraintPriority.ASSIGNMENT)
            }


            element is KtVariableDeclaration -> {
                inferenceContext.declarationToTypeVariable[element]?.also { typeVariable ->
                    element.initializer?.isSubtypeOf(typeVariable, ConstraintPriority.INITIALIZER)
                }
            }

            element is KtParameter -> {
                inferenceContext.declarationToTypeVariable[element]?.also { typeVariable ->
                    element.defaultValue?.isSubtypeOf(
                        typeVariable,
                        ConstraintPriority.INITIALIZER
                    )
                }
            }

            element is KtReturnExpression -> {
                val functionTypeVariable = element.getTargetFunction(element.analyze(resolutionFacade))
                    ?.resolveToDescriptorIfAny(resolutionFacade)
                    ?.let { functionDescriptor ->
                        inferenceContext.declarationDescriptorToTypeVariable[functionDescriptor]
                    } ?: return
                element.returnedExpression?.isSubtypeOf(
                    functionTypeVariable,
                    ConstraintPriority.RETURN
                )
            }

            element is KtReturnExpression -> {
                val targetTypeVariable =
                    element.getTargetFunction(element.analyze(resolutionFacade))?.let { function ->
                        inferenceContext.declarationToTypeVariable[function]
                    }
                if (targetTypeVariable != null) {
                    element.returnedExpression?.isSubtypeOf(
                        targetTypeVariable,
                        ConstraintPriority.RETURN
                    )
                }
            }

            element is KtLambdaExpression -> {
                val targetTypeVariable =
                    inferenceContext.declarationToTypeVariable[element.functionLiteral] ?: return
                element.functionLiteral.bodyExpression?.statements?.lastOrNull()
                    ?.takeIf { it !is KtReturnExpression }
                    ?.also { implicitReturn ->
                        implicitReturn.isSubtypeOf(
                            targetTypeVariable,
                            ConstraintPriority.RETURN
                        )
                    }
            }

            element is KtForExpression -> {
                val loopParameterTypeVariable =
                    element.loopParameter?.typeReference?.typeElement?.let { typeElement ->
                        inferenceContext.typeElementToTypeVariable[typeElement]
                    }
                if (loopParameterTypeVariable != null) {
                    val loopRangeBoundType = element.loopRange?.boundType(inferenceContext) ?: return
                    val boundType =
                        element.loopRangeElementType(resolutionFacade)
                            ?.boundType(
                                contextBoundType = loopRangeBoundType,
                                inferenceContext = inferenceContext
                            ) ?: return
                    loopParameterTypeVariable.isSubtypeOf(
                        boundType.typeParameters.firstOrNull()?.boundType ?: return,
                        ConstraintPriority.ASSIGNMENT
                    )
                }
            }
        }
        Unit
    }

    private fun KtForExpression.loopRangeElementType(resolutionFacade: ResolutionFacade): KotlinType? {
        val loopRangeType = loopRange?.getType(analyze(resolutionFacade)) ?: return null
        return loopRangeType
            .constructor
            .declarationDescriptor
            ?.safeAs<ClassDescriptor>()
            ?.getMemberScope(loopRangeType.arguments)
            ?.getDescriptorsFiltered(DescriptorKindFilter.FUNCTIONS) {
                it.asString() == "iterator"
            }?.filterIsInstance<FunctionDescriptor>()
            ?.firstOrNull { it.valueParameters.isEmpty() }
            ?.original
            ?.returnType
    }
}