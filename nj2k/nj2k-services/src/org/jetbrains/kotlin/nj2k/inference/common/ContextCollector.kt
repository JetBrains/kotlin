/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class ContextCollector(private val resolutionFacade: ResolutionFacade) {
    private fun KotlinType.classReference(): ClassReference? =
        when (val descriptor = constructor.declarationDescriptor) {
            is ClassDescriptor -> descriptor.classReference
            is TypeParameterDescriptor -> TypeParameterReference(descriptor)
            else -> null
        }

    private fun KtTypeElement.toData(): TypeElementData? {
        val typeReference = parent as? KtTypeReference ?: return null
        val type = analyze(resolutionFacade)[BindingContext.TYPE, typeReference] ?: return null
        val typeParameterDescriptor = type.constructor
            .declarationDescriptor
            ?.safeAs<TypeParameterDescriptor>() ?: return TypeElementDataImpl(this, type)
        return TypeParameterElementData(this, type, typeParameterDescriptor)
    }

    fun collectTypeVariables(elements: List<KtElement>): InferenceContext {
        val declarationToTypeVariable = mutableMapOf<KtNamedDeclaration, TypeVariable>()
        val typeElementToTypeVariable = mutableMapOf<KtTypeElement, TypeVariable>()
        val typeBasedTypeVariables = mutableListOf<TypeBasedTypeVariable>()

        fun KtTypeReference.toBoundType(
            owner: TypeVariableOwner,
            alreadyCalculatedType: KotlinType? = null,
            defaultState: State? = null
        ): BoundType? {
            val typeElement = typeElement ?: return null
            val type = alreadyCalculatedType ?: analyze(resolutionFacade, BodyResolveMode.PARTIAL)[BindingContext.TYPE, this]
            val classReference = type?.classReference() ?: NoClassReference
            val state = defaultState ?: classReference.getState(typeElement)

            val typeArguments =
                if (classReference is DescriptorClassReference) {
                    val typeParameters = classReference.descriptor.declaredTypeParameters
                    typeElement.typeArgumentsAsTypes.mapIndexed { index, typeArgument ->
                        TypeParameter(
                            typeArgument?.toBoundType(
                                owner,
                                alreadyCalculatedType = type?.arguments?.getOrNull(index)?.type
                            ) ?: BoundType.STAR_PROJECTION,
                            typeParameters.getOrNull(index)?.variance ?: Variance.INVARIANT
                        )
                    }
                } else emptyList()

            val typeVariable = TypeElementBasedTypeVariable(
                classReference,
                typeArguments,
                typeElement.toData() ?: return null,
                owner,
                state
            )
            typeElementToTypeVariable[typeElement] = typeVariable
            return typeVariable.asBoundType()
        }

        fun KotlinType.toBoundType(): BoundType? {
            val classReference = classReference() ?: NoClassReference
            val state = classReference.getState(typeElement = null)

            val typeArguments =
                if (classReference is DescriptorClassReference) {
                    arguments.zip(classReference.descriptor.declaredTypeParameters) { typeArgument, typeParameter ->
                        TypeParameter(
                            typeArgument.type.toBoundType() ?: BoundType.STAR_PROJECTION,
                            typeParameter.variance
                        )
                    }
                } else emptyList()


            val typeVariable = TypeBasedTypeVariable(
                classReference,
                typeArguments,
                this,
                state
            )
            typeBasedTypeVariables += typeVariable
            return typeVariable.asBoundType()
        }

        val substitutors = mutableMapOf<ClassDescriptor, SuperTypesSubstitutor>()

        fun isOrAsExpression(typeReference: KtTypeReference) {
            val typeElement = typeReference.typeElement ?: return
            val typeVariable = typeReference.toBoundType(OtherTarget)?.typeVariable ?: return
            typeElementToTypeVariable[typeElement] = typeVariable
        }

        for (element in elements) {
            element.forEachDescendantOfType<KtExpression> { expression ->
                if (expression is KtCallableDeclaration
                    && (expression is KtParameter
                            || expression is KtProperty
                            || expression is KtNamedFunction)
                ) run {
                    val typeReference = expression.typeReference ?: return@run
                    val typeVariable = typeReference.toBoundType(
                        when (expression) {
                            is KtParameter -> expression.getStrictParentOfType<KtFunction>()?.let(::FunctionParameter)
                            is KtFunction -> FunctionReturnType(expression)
                            is KtProperty -> Property(expression)
                            else -> null
                        } ?: OtherTarget
                    )?.typeVariable ?: return@run
                    declarationToTypeVariable[expression] = typeVariable
                }

                if (expression is KtTypeParameterListOwner) {
                    for (typeParameter in expression.typeParameters) {
                        typeParameter.extendsBound?.toBoundType(OtherTarget, defaultState = State.UPPER)
                    }
                    for (constraint in expression.typeConstraints) {
                        constraint.boundTypeReference?.toBoundType(OtherTarget, defaultState = State.UPPER)
                    }
                }

                when (expression) {
                    is KtClassOrObject -> {
                        for (entry in expression.superTypeListEntries) {
                            for (argument in entry.typeReference?.typeElement?.typeArgumentsAsTypes ?: continue) {
                                argument?.toBoundType(OtherTarget)
                            }
                        }
                        val descriptor =
                            expression.resolveToDescriptorIfAny(resolutionFacade) ?: return@forEachDescendantOfType
                        substitutors[descriptor] =
                            SuperTypesSubstitutor.createFromKtClass(expression, resolutionFacade) ?: return@forEachDescendantOfType
                        for (typeParameter in expression.typeParameters) {
                            val typeVariable = typeParameter.resolveToDescriptorIfAny(resolutionFacade)
                                ?.safeAs<TypeParameterDescriptor>()
                                ?.defaultType
                                ?.toBoundType()
                                ?.typeVariable
                                ?: continue
                            declarationToTypeVariable[typeParameter] = typeVariable
                        }
                    }
                    is KtCallExpression ->
                        for (typeArgument in expression.typeArguments) {
                            typeArgument.typeReference?.toBoundType(TypeArgument)
                        }
                    is KtLambdaExpression -> {
                        val context = expression.analyze(resolutionFacade)
                        val returnType = expression.getType(context)?.arguments?.lastOrNull()?.type ?: return@forEachDescendantOfType
                        val typeVariable = returnType.toBoundType()?.typeVariable ?: return@forEachDescendantOfType
                        declarationToTypeVariable[expression.functionLiteral] = typeVariable
                    }
                    is KtBinaryExpressionWithTypeRHS -> {
                        isOrAsExpression(expression.right ?: return@forEachDescendantOfType)

                    }
                    is KtIsExpression -> {
                        isOrAsExpression(expression.typeReference ?: return@forEachDescendantOfType)
                    }
                }
            }
        }

        val typeVariables =
            (typeElementToTypeVariable.values + declarationToTypeVariable.values + typeBasedTypeVariables).distinct()
        return InferenceContext(
            elements,
            typeVariables,
            typeElementToTypeVariable,
            declarationToTypeVariable,
            declarationToTypeVariable.mapNotNull { (key, value) ->
                key.resolveToDescriptorIfAny(resolutionFacade)?.let { it to value }
            }.toMap(),
            substitutors
        )
    }

    abstract fun ClassReference.getState(typeElement: KtTypeElement?): State
}