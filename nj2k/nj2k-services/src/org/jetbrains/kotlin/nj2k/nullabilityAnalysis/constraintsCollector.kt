/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunction
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


internal class LabelToFunctionTypeVariableMapper(analysisContext: AnalysisContext) {
    private val labelToFunction = analysisContext.declarationToTypeVariable.mapNotNull { (declaration, _) ->
        if (declaration !is KtNamedFunction) return@mapNotNull null
        val label = declaration.nameIdentifier?.getLabel() ?: return@mapNotNull null
        label to declaration
    }.toMap()

    fun functionByLabel(label: JKElementInfoLabel): KtNamedFunction? =
        labelToFunction[label]

}

internal class ConstraintsCollector(
    private val analysisContext: AnalysisContext,
    private val converterContext: NewJ2kConverterContext?,
    printConstraints: Boolean
) {
    private val boundTypeStorage = BoundTypeStorage(analysisContext, printConstraints)
    private val constraintBuilder = ConstraintBuilder(boundTypeStorage)
    private val labelToFunctionTypeVariableMapper = LabelToFunctionTypeVariableMapper(analysisContext)

    private val PsiElement.elementInfo: List<JKElementInfo>?
        get() = getLabel()?.let { label ->
            converterContext?.elementsInfoStorage?.getInfoForLabel(label)
        }

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
                expression.right?.addSubtypeNullabilityConstraint(expression.left ?: return, ConstraintCameFrom.ASSIGNMENT_TARGET)
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

            expression is KtForExpression -> {
                expression.loopRange?.addEqualsNullabilityConstraint(Nullability.NOT_NULL, ConstraintCameFrom.INITIALIZER)

                val loopParameterTypeVariable =
                    expression.loopParameter?.typeReference?.typeElement?.let { typeElement ->
                        analysisContext.typeElementToTypeVariable[typeElement]
                    }
                if (loopParameterTypeVariable != null) {
                    val loopRangeBoundType = boundTypeStorage.boundTypeFor(expression.loopRange ?: return)
                    val loopRangeType = expression.loopRange?.getType(expression.analyze()) ?: return
                    val loopRangeItemType = loopRangeType
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
                        ?: return
                    val boundType = boundTypeStorage.boundTypeForType(
                        loopRangeItemType,
                        loopRangeBoundType,
                        emptyMap()
                    ) ?: return
                    loopParameterTypeVariable.addEqualsNullabilityConstraint(
                        boundType.typeParameters.firstOrNull()?.boundType ?: return,
                        ConstraintCameFrom.ASSIGNMENT_TARGET
                    )
                }
            }
        }
        Unit
    }


    private fun SuperFunctionInfo.superFunctionBoundType(
        originalFunctionDescriptor: FunctionDescriptor
    ): BoundType? =
        when (this) {
            is ExternalSuperFunctionInfo -> {
                val containingSuperClassBoundType =
                    descriptor.containingDeclaration.safeAs<ClassDescriptor>()?.let { klass ->
                        GenericBoundType(
                            DescriptorClassReference(klass),
                            emptyList(),//TODO,
                            isNull = false
                        )
                    }
                originalFunctionDescriptor.returnType?.let { type ->
                    boundTypeStorage.boundTypeForType(type, containingSuperClassBoundType, emptyMap()/*TODO*/)
                }
            }
            is InternalSuperFunctionInfo -> {
                labelToFunctionTypeVariableMapper.functionByLabel(label)?.let { function ->
                    analysisContext.declarationToTypeVariable[function]?.let { TypeVariableBoundType(it) }
                }
            }
        }

    private fun SuperFunctionInfo.superFunctionParameterBoundType(parameterIndex: Int): BoundType? {
        return when (this) {
            is ExternalSuperFunctionInfo -> {
                val containingSuperClassBoundType =
                    descriptor.containingDeclaration.safeAs<ClassDescriptor>()?.let { klass ->
                        GenericBoundType(
                            DescriptorClassReference(klass),
                            emptyList(),//TODO,
                            isNull = false
                        )
                    }
                val parameter = descriptor.valueParameters.getOrNull(parameterIndex) ?: return null
                boundTypeStorage.boundTypeForType(parameter.type, containingSuperClassBoundType, emptyMap()/*TODO*/)
            }
            is InternalSuperFunctionInfo -> {
                val function = labelToFunctionTypeVariableMapper.functionByLabel(label) ?: return null
                val parameter = function.valueParameters.getOrNull(parameterIndex) ?: return null
                analysisContext.declarationToTypeVariable[parameter]?.let { TypeVariableBoundType(it) }
            }
        }
    }


    private fun collectSuperDeclarationConstraintsForFunction(function: KtNamedFunction) = with(constraintBuilder) {
        val (descriptor, superDeclarationDescriptors) =
            function.nameIdentifier?.elementInfo?.firstIsInstanceOrNull<FunctionInfo>() ?: return@with

        for (superDeclarationInfo in superDeclarationDescriptors) {
            val superFunctionBoundType = superDeclarationInfo.superFunctionBoundType(descriptor) ?: continue

            analysisContext.declarationToTypeVariable[function]?.addEqualsNullabilityConstraint(
                superFunctionBoundType,
                ConstraintCameFrom.SUPER_DECLARATION
            )

            for (parameterIndex in function.valueParameters.indices) {
                val parameterTypeVariable =
                    function.valueParameters[parameterIndex].let { analysisContext.declarationToTypeVariable[it] } ?: return
                val superParameterBoundType = superDeclarationInfo.superFunctionParameterBoundType(parameterIndex) ?: return
                parameterTypeVariable.addEqualsNullabilityConstraint(superParameterBoundType, ConstraintCameFrom.SUPER_DECLARATION)
            }
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
                    .boundTypeForType(type, receiverBoundType, callExpression.typeArgumentsDescriptors(descriptor).orEmpty())
            }


        for (argumentIndex in callExpression.valueArguments.indices) {
            val valueArgument = callExpression.valueArguments[argumentIndex]
            val argument = valueArgument.getArgumentExpression() ?: continue

            if (valueArgument.isSpread) {
                argument.addEqualsNullabilityConstraint(Nullability.NOT_NULL, ConstraintCameFrom.USED_AS_RECEIVER)
            }

            argument.addSubtypeNullabilityConstraint(
                parameterBoundTypeIndex(argumentIndex) ?: continue,
                ConstraintCameFrom.PARAMETER_PASSED
            )
        }
    }

    private fun KtCallExpression.typeArgumentsDescriptors(descriptor: CallableDescriptor): Map<TypeParameterDescriptor, TypeVariable>? {
        return descriptor.typeParameters.zip(typeArguments) { typeParameter, typeArgument ->
            val typeVariable =
                analysisContext.typeElementToTypeVariable[typeArgument.typeReference?.typeElement ?: return null]
                    ?: return null
            typeParameter to typeVariable
        }.toMap()
    }
}