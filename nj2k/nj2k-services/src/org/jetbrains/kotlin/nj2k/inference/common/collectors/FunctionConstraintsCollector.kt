/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common.collectors

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.nj2k.inference.common.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FunctionConstraintsCollector(
    private val superFunctionsProvider: SuperFunctionsProvider
) : ConstraintsCollector() {
    override fun ConstraintBuilder.collectConstraints(
        element: KtElement,
        boundTypeCalculator: BoundTypeCalculator,
        inferenceContext: InferenceContext,
        resolutionFacade: ResolutionFacade
    ) {
        if (element !is KtFunction) return
        superFunctionsProvider.inferenceContext = inferenceContext
        val ktClass = element.containingClassOrObject ?: return
        val classDescriptor = ktClass.resolveToDescriptorIfAny(resolutionFacade) ?: return

        val superFunctions = superFunctionsProvider.provideSuperFunctionDescriptors(element) ?: return
        val substitutor = inferenceContext.classSubstitutions[classDescriptor] ?: return

        for (superFunction in superFunctions) {
            val superClass = superFunction.containingDeclaration as? ClassDescriptor ?: continue
            val superFunctionPsi = superFunction.original.findPsi() as? KtNamedFunction

            run {
                collectTypeConstraints(
                    element.typeReference?.typeElement ?: return@run,
                    superFunction.original.returnType ?: return@run,
                    superFunctionPsi?.typeReference?.typeElement,
                    substitutor,
                    superClass,
                    inferenceContext
                )
            }

            for (parameterIndex in element.valueParameters.indices) {
                collectTypeConstraints(
                    element.valueParameters.getOrNull(parameterIndex)?.typeReference?.typeElement ?: continue,
                    superFunction.valueParameters.getOrNull(parameterIndex)?.original?.type ?: continue,
                    superFunctionPsi?.valueParameters?.getOrNull(parameterIndex)?.typeReference?.typeElement,
                    substitutor,
                    superClass,
                    inferenceContext
                )
            }
        }
    }

    private fun ConstraintBuilder.collectTypeConstraints(
        typeElement: KtTypeElement,
        superType: KotlinType,
        superTypeElement: KtTypeElement?,
        substitutor: ClassSubstitutor,
        superClass: ClassDescriptor,
        inferenceContext: InferenceContext
    ) {
        val usedTypeVariables = hashSetOf<TypeVariable>()
        val substitutions = calculateTypeSubstitutions(typeElement, superType) ?: return
        for ((innerTypeElement, innerTypeParameter) in substitutions) {
            val superEntryTypeElement = substitutor[superClass, innerTypeParameter] ?: continue
            innerTypeElement.isTheSameTypeAs(superEntryTypeElement, ConstraintPriority.SUPER_DECLARATION)

            inferenceContext.typeElementToTypeVariable[innerTypeElement]?.also { usedTypeVariables += it }
        }
        val superTypeVariable = superTypeElement?.let { inferenceContext.typeElementToTypeVariable[it] }
        if (superTypeVariable != null) {
            superTypeVariable.isTheSameTypeAs(typeElement, ConstraintPriority.SUPER_DECLARATION, usedTypeVariables)
        } else {
            superType.boundType(forceEnhance = true, inferenceContext = inferenceContext)
                .isTheSameTypeAs(typeElement, ConstraintPriority.SUPER_DECLARATION, usedTypeVariables)
        }

    }

    private fun calculateTypeSubstitutions(
        typeElement: KtTypeElement,
        superType: KotlinType
    ): List<Pair<KtTypeElement, TypeParameterDescriptor>>? {
        val substitution = superType.constructor.declarationDescriptor
            ?.safeAs<TypeParameterDescriptor>()?.let { typeElement to it }
        return typeElement.typeArgumentsAsTypes.zip(superType.arguments)
            .flatMap { (argumentTypeElement, argumentType) ->
                calculateTypeSubstitutions(argumentTypeElement?.typeElement ?: return null, argumentType.type) ?: return null
            } + listOfNotNull(substitution)
    }

}