/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common.collectors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.nj2k.inference.common.*
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class CallExpressionConstraintCollector : ConstraintsCollector() {
    override fun ConstraintBuilder.collectConstraints(
        element: KtElement,
        boundTypeCalculator: BoundTypeCalculator,
        inferenceContext: InferenceContext,
        resolutionFacade: ResolutionFacade
    ) {
        if (element !is KtCallElement) return
        val call = element.resolveToCall(resolutionFacade) ?: return
        val originalDescriptor = call.candidateDescriptor.safeAs<FunctionDescriptor>()?.original ?: return
        val valueArguments = call.valueArgumentsByIndex.orEmpty()
        val typeParameterBindings =
            call.candidateDescriptor.typeParameters.zip(call.call.typeArguments) { typeParameter, typeArgument ->
                typeArgument.typeReference?.typeElement?.let {
                    inferenceContext.typeElementToTypeVariable[it]
                }?.let { typeVariable ->
                    typeParameter?.let { it to typeVariable }
                }
            }.filterNotNull().toMap()

        val receiverExpressionBoundType = element.getQualifiedExpressionForSelector()
            ?.receiverExpression
            ?.boundType(inferenceContext)

        fun KotlinType.contextBoundType() = boundType(
            contextBoundType = receiverExpressionBoundType,
            call = call,
            inferenceContext = inferenceContext
        )

        fun BoundType.substituteTypeParameters(): BoundType =
            BoundTypeImpl(
                when (label) {
                    is TypeVariableLabel -> (label as TypeVariableLabel)
                        .typeVariable
                        .safeAs<TypeElementBasedTypeVariable>()
                        ?.typeElement
                        ?.safeAs<TypeParameterElementData>()
                        ?.let { typeParameterData ->
                            typeParameterBindings[typeParameterData.typeParameterDescriptor]
                        }?.let { typeVariable ->
                            TypeVariableLabel(typeVariable)
                        } ?: label
                    else -> label
                },
                typeParameters.map { typeParameter ->
                    TypeParameter(
                        typeParameter.boundType.substituteTypeParameters(),
                        typeParameter.variance
                    )
                }
            ).withEnhancementFrom(this)

        fun ParameterDescriptor.boundType() =
            inferenceContext.declarationDescriptorToTypeVariable[this]
                ?.asBoundType()
                ?.substituteTypeParameters()
                ?: original.type.contextBoundType()


        if (receiverExpressionBoundType != null) run {
            val receiverBoundType =
                (originalDescriptor.extensionReceiverParameter ?: originalDescriptor.dispatchReceiverParameter)?.boundType()
                    ?: return@run
            receiverExpressionBoundType.isSubtypeOf(receiverBoundType, ConstraintPriority.RECEIVER_PARAMETER)
        }

        val parameterToArgument = call.candidateDescriptor.valueParameters.let { parameters ->
            valueArguments.mapIndexed { i, arguments ->
                val parameter = parameters[i]
                val parameterBoundType = parameter.boundType()

                val parameterBoundTypeConsideringVararg =
                    if (parameter.isVararg && KotlinBuiltIns.isArrayOrPrimitiveArray(parameter.type)) {
                        if (KotlinBuiltIns.isPrimitiveArray(parameter.type))
                            BoundTypeImpl(
                                GenericLabel(NoClassReference),//not important as it just a primitive type
                                emptyList()
                            ) else parameterBoundType.typeParameters[0].boundType
                    } else parameterBoundType
                arguments.arguments.map { argument ->
                    parameterBoundTypeConsideringVararg to argument
                }
            }
        }.flatten()

        for ((parameter, argument) in parameterToArgument) {
            val argumentExpression = argument.getArgumentExpression() ?: continue
            argumentExpression.isSubtypeOf(parameter, ConstraintPriority.PARAMETER)
        }
    }
}