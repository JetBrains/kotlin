/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunction
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.descriptorUtil.firstOverridden
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class ConstraintsCollector(
    private val analysisContext: AnalysisContext,
    printConstraints: Boolean
) {
    private val boundTypeStorage = BoundTypeStorage(analysisContext, printConstraints)
    private val constraintBuilder = ConstraintBuilder(boundTypeStorage)

    private inline val KtExpression.boundType
        get() = boundTypeStorage.boundTypeFor(this)


    internal fun collectConstraints(analysisScope: AnalysisScope): List<Constraint> {
        analysisScope.forEach { element ->
            element.forEachDescendantOfType<KtExpression> { expression ->
                collectConstraintsForExpression(expression)
            }
        }
        return constraintBuilder.getConstraints()
    }

    private fun collectConstraintsForExpression(expression: KtExpression) = with(constraintBuilder) {
        when {
            expression is KtQualifiedExpression -> {
                expression.receiverExpression.addEqualsNullabilityConstraint(Nullability.NOT_NULL, ConstraintCameFrom.USED_AS_RECEIVER)
            }

            expression is KtBinaryExpressionWithTypeRHS && KtPsiUtil.isUnsafeCast(expression) -> {
                expression.right?.typeElement?.let { analysisContext.typeElementToTypeVariable[it] }?.also { typeVariable ->
                    expression.left.addSubtypeNullabilityConstraint(typeVariable, ConstraintCameFrom.ASSIGNMENT_TARGET)
                }
            }

            expression is KtBinaryExpression && expression.asAssignment() != null -> {
                expression.right?.addSubtypeNullabilityConstraint(expression.left!!, ConstraintCameFrom.ASSIGNMENT_TARGET)
            }

            expression is KtBinaryExpression && expression.isComaprationWithNull() -> {
                val comparedExpression =
                    if (expression.left.isNullExpression()) expression.right
                    else expression.left
                comparedExpression?.addEqualsNullabilityConstraint(Nullability.NULLABLE, ConstraintCameFrom.COMPARED_WITH_NULL)
            }

            expression is KtBinaryExpression -> {
                expression.left?.addEqualsNullabilityConstraint(Nullability.NOT_NULL, ConstraintCameFrom.USED_AS_RECEIVER)
                expression.right?.addEqualsNullabilityConstraint(Nullability.NOT_NULL, ConstraintCameFrom.USED_AS_RECEIVER)
            }

            expression is KtProperty -> {
                analysisContext.declarationToTypeVariable[expression]?.also { typeVariable ->
                    expression.initializer?.addSubtypeNullabilityConstraint(
                        typeVariable,
                        ConstraintCameFrom.INITIALIZER
                    )
                }
            }

            expression is KtParameter -> {
                analysisContext.declarationToTypeVariable[expression]?.also { typeVariable ->
                    expression.defaultValue?.addSubtypeNullabilityConstraint(
                        typeVariable,
                        ConstraintCameFrom.INITIALIZER
                    )
                }
            }

            expression is KtIfExpression -> {
                expression.condition?.addEqualsNullabilityConstraint(Nullability.NOT_NULL, ConstraintCameFrom.INITIALIZER)
            }

            expression is KtWhileExpression -> {
                expression.condition?.addEqualsNullabilityConstraint(Nullability.NOT_NULL, ConstraintCameFrom.INITIALIZER)
            }

            expression is KtForExpression -> {
                expression.loopRange?.addEqualsNullabilityConstraint(Nullability.NOT_NULL, ConstraintCameFrom.INITIALIZER)
            }

            expression is KtCallExpression -> {
                collectConstraintsForCallExpression(
                    expression,
                    expression.parent?.safeAs<KtQualifiedExpression>()?.receiverExpression
                )
            }

            expression is KtReturnExpression -> {
                val targetTypeVariable = expression.getTargetFunction(expression.analyze())?.let { function ->
                    analysisContext.declarationToTypeVariable[function]
                }
                if (targetTypeVariable != null) {
                    expression.returnedExpression?.addSubtypeNullabilityConstraint(
                        targetTypeVariable,
                        ConstraintCameFrom.ASSIGNMENT_TARGET
                    )
                }
            }

            expression is KtNamedFunction -> {
                collectSuperDeclarationConstraintsForFunction(expression)
            }
        }
        Unit
    }


    private fun collectSuperDeclarationConstraintsForFunction(function: KtNamedFunction) = with(constraintBuilder) {
        val descriptor = function.resolveToDescriptorIfAny() ?: return
        val superDeclarationDescriptor = descriptor.firstOverridden { true }
            ?.takeIf { it != descriptor }
            ?.safeAs<FunctionDescriptor>()
            ?: return

        val containingSuperClassBoundType =
            superDeclarationDescriptor.containingDeclaration.safeAs<ClassDescriptor>()?.let { klass ->
                GenericBoundType(
                    DescriptorClassReference(klass),
                    emptyList(),//TODO,
                    isNull = false
                )
            }

        val superFunctionPsi = superDeclarationDescriptor.findPsi() as? KtNamedFunction

        val superFunctionBoundType =
            superFunctionPsi?.let { analysisContext.declarationToTypeVariable[it] }?.let {
                TypeVariableBoundType(it)
            } ?: descriptor.returnType?.let { type ->
                boundTypeStorage.boundTypeForType(type, containingSuperClassBoundType, emptyMap()/*TODO*/)
            } ?: return

        analysisContext.declarationToTypeVariable[function]?.addEqualsNullabilityConstraint(
            superFunctionBoundType,
            ConstraintCameFrom.SUPER_DECLARATION
        )

        for (parameterIndex in function.valueParameters.indices) {
            val parameterTypeVariable =
                function.valueParameters[parameterIndex].let { analysisContext.declarationToTypeVariable[it] } ?: return

            val superParameterBoundType = superFunctionPsi?.valueParameters?.get(parameterIndex)
                ?.let { analysisContext.declarationToTypeVariable[it] }
                ?.let { TypeVariableBoundType(it) }
                ?: superDeclarationDescriptor.valueParameters.getOrNull(parameterIndex)
                    ?.let { boundTypeStorage.boundTypeForType(it.type, containingSuperClassBoundType, emptyMap()/*TODO*/) }
                ?: continue
            parameterTypeVariable.addEqualsNullabilityConstraint(superParameterBoundType, ConstraintCameFrom.SUPER_DECLARATION)
        }
    }


    private fun collectConstraintsForCallExpression(callExpression: KtCallExpression, receiver: KtExpression?) = with(constraintBuilder) {
        val receiverBoundType = receiver?.boundType
        val descriptor = callExpression.resolveToCall()?.candidateDescriptor?.original?.safeAs<CallableDescriptor>() ?: return
        val function = descriptor.findPsi()?.safeAs<KtNamedFunction>()

        fun parameterBoundTypeIndex(index: Int): BoundType? =
            function?.valueParameters?.let { parameters ->
                val parameter =
                    if (index <= parameters.lastIndex) parameters[index]
                    else parameters.lastOrNull()?.takeIf { parameter ->
                        parameter.isVarArg
                    }
                val typeVariable = parameter?.let { analysisContext.declarationToTypeVariable[it] }
                typeVariable?.let {
                    TypeVariableBoundType(it)
                }
            } ?: run {
                if (index < descriptor.valueParameters.lastIndex
                    || index == descriptor.valueParameters.lastIndex && descriptor.valueParameters.lastOrNull()?.isVararg == false
                ) descriptor.valueParameters[index].type
                else {
                    descriptor.valueParameters.lastOrNull()?.takeIf { parameter ->
                        parameter.isVararg && KotlinBuiltIns.isArray(parameter.type)
                    }?.let { parameter ->
                        parameter.type.arguments.singleOrNull()?.type
                    }
                }
            }?.let { type ->
                boundTypeStorage
                    .boundTypeForType(type, receiverBoundType, callExpression.typeArgumentsDescriptors(descriptor))
            }


        for (argumentIndex in callExpression.valueArguments.indices) {
            val argument = callExpression.valueArguments[argumentIndex].getArgumentExpression() ?: continue

            argument.addSubtypeNullabilityConstraint(
                parameterBoundTypeIndex(argumentIndex) ?: continue,
                ConstraintCameFrom.PARAMETER_PASSED
            )
        }
    }

    private fun KtCallExpression.typeArgumentsDescriptors(descriptor: CallableDescriptor): Map<TypeParameterDescriptor, TypeVariable> =
        descriptor.typeParameters.zip(typeArguments) { typeParameter, typeArgument ->
            typeParameter to
                    this@ConstraintsCollector.analysisContext.typeElementToTypeVariable.getValue(typeArgument.typeReference?.typeElement!!)
        }.toMap()
}