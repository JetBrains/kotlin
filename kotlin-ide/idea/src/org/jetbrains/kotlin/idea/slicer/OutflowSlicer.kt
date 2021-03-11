/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector.Access
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.util.parentOfType
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ReturnValueInstruction
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingElement
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReadWriteAccessDetector
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class OutflowSlicer(
    element: KtElement,
    processor: Processor<in SliceUsage>,
    parentUsage: KotlinSliceUsage
) : Slicer(element, processor, parentUsage) {

    override fun processChildren(forcedExpressionMode: Boolean) {
        if (forcedExpressionMode) {
            (element as? KtExpression)?.let { processExpression(it) }
            return
        }

        when (element) {
            is KtProperty -> processVariable(element)

            is KtParameter -> processVariable(element)

            is KtFunction -> processFunction(element)

            is KtPropertyAccessor -> {
                if (element.isGetter) {
                    processVariable(element.property)
                }
            }

            is KtTypeReference -> {
                val declaration = element.parent
                require(declaration is KtCallableDeclaration)
                require(element == declaration.receiverTypeReference)

                if (declaration.isExpectDeclaration()) {
                    declaration.resolveToDescriptorIfAny()
                        ?.actualsForExpected()
                        ?.forEach {
                            val actualDeclaration = (it as? DeclarationDescriptorWithSource)?.toPsi()
                            (actualDeclaration as? KtCallableDeclaration)?.receiverTypeReference?.passToProcessor()
                        }
                }

                when (declaration) {
                    is KtFunction -> {
                        processExtensionReceiverUsages(declaration, declaration.bodyExpression, mode)
                    }

                    is KtProperty -> {
                        //TODO: process only one of them or both depending on the usage type
                        processExtensionReceiverUsages(declaration, declaration.getter?.bodyExpression, mode)
                        processExtensionReceiverUsages(declaration, declaration.setter?.bodyExpression, mode)
                    }
                }
            }

            is KtExpression -> processExpression(element)
        }
    }

    private fun processVariable(variable: KtCallableDeclaration) {
        val withDereferences = parentUsage.params.showInstanceDereferences
        val accessKind = if (withDereferences) AccessKind.READ_OR_WRITE else AccessKind.READ_ONLY

        fun processVariableAccess(usageInfo: UsageInfo) {
            val refElement = usageInfo.element ?: return
            if (refElement !is KtExpression) {
                if (refElement.parentOfType<PsiComment>() == null) {
                    refElement.passToProcessor()
                }
                return
            }

            if (refElement.parent is KtValueArgumentName) return // named argument reference is not a read or write

            val refExpression = KtPsiUtil.safeDeparenthesize(refElement)
            if (withDereferences) {
                refExpression.processDereferences()
            }
            if (!withDereferences || KotlinReadWriteAccessDetector.INSTANCE.getExpressionAccess(refExpression) == Access.Read) {
                refExpression.passToProcessor()
            }
        }

        var searchScope = analysisScope

        if (variable is KtParameter) {
            if (!canProcessParameter(variable)) return //TODO

            val callable = variable.ownerFunction as? KtCallableDeclaration

            if (callable != null) {
                if (callable.isExpectDeclaration()) {
                    variable.resolveToDescriptorIfAny()
                        ?.actualsForExpected()
                        ?.forEach {
                            (it as? DeclarationDescriptorWithSource)?.toPsi()?.passToProcessor()
                        }
                }

                val parameterIndex = variable.parameterIndex()
                callable.forEachOverridingElement(scope = analysisScope) { _, overridingMember ->
                    when (overridingMember) {
                        is KtCallableDeclaration -> {
                            val parameters = overridingMember.valueParameters
                            check(parameters.size == callable.valueParameters.size)
                            parameters[parameterIndex].passToProcessor()
                        }

                        is PsiMethod -> {
                            val parameters = overridingMember.parameterList.parameters
                            val shift = if (callable.receiverTypeReference != null) 1 else 0
                            check(parameters.size == callable.valueParameters.size + shift)
                            parameters[parameterIndex + shift].passToProcessor()
                        }

                        else -> {
                            // not supported
                        }
                    }
                    true
                }

                if (callable is KtNamedFunction) { // references to parameters of inline function can be outside analysis scope
                    searchScope = LocalSearchScope(callable)
                }
            }
        }

        processVariableAccesses(variable, searchScope, accessKind, ::processVariableAccess)
    }

    private fun processFunction(function: KtFunction) {
        processCalls(function, includeOverriders = false, CallSliceProducer)
    }

    private fun processExpression(expression: KtExpression) {
        val expressionWithValue = when (expression) {
            is KtFunctionLiteral -> expression.parent as KtLambdaExpression
            else -> expression
        }
        expressionWithValue.processPseudocodeUsages { pseudoValue, instruction ->
            when (instruction) {
                is WriteValueInstruction -> {
                    if (!pseudoValue.processIfReceiverValue(instruction, mode)) {
                        instruction.target.accessedDescriptor?.toPsi()?.passToProcessor()
                    }
                }

                is ReadValueInstruction -> {
                    pseudoValue.processIfReceiverValue(instruction, mode)
                }

                is CallInstruction -> {
                    if (!pseudoValue.processIfReceiverValue(instruction, mode)) {
                        val parameterDescriptor = instruction.arguments[pseudoValue] ?: return@processPseudocodeUsages
                        val parameter = parameterDescriptor.toPsi()
                        if (parameter != null) {
                            parameter.passToProcessorInCallMode(instruction.element)
                        } else {
                            val function = parameterDescriptor.containingDeclaration as? FunctionDescriptor
                                ?: return@processPseudocodeUsages
                            if (function.isImplicitInvokeFunction()) {
                                processImplicitInvokeCall(instruction, parameterDescriptor)
                            }
                        }
                    }
                }

                is ReturnValueInstruction -> {
                    val subroutine = instruction.subroutine
                    if (subroutine is KtNamedFunction) {
                        val (newMode, callElement) = mode.popInlineFunctionCall(subroutine)
                        if (newMode != null) {
                            callElement?.passToProcessor(newMode)
                            return@processPseudocodeUsages
                        }
                    }

                    subroutine.passToProcessor()
                }

                is MagicInstruction -> {
                    when (instruction.kind) {
                        MagicKind.NOT_NULL_ASSERTION, MagicKind.CAST -> instruction.outputValue.element?.passToProcessor()
                        else -> {
                        }
                    }
                }
            }
        }
    }

    private fun processImplicitInvokeCall(instruction: CallInstruction, parameterDescriptor: ValueParameterDescriptor) {
        val receiverPseudoValue = instruction.receiverValues.entries.singleOrNull()?.key ?: return
        val receiverExpression = receiverPseudoValue.element as? KtExpression ?: return
        val bindingContext = receiverExpression.analyze(BodyResolveMode.PARTIAL)
        var receiverType = bindingContext.getType(receiverExpression)
        var receiver: PsiElement = receiverExpression
        if (receiverType == null && receiverExpression is KtReferenceExpression) {
            val targetDescriptor = bindingContext[BindingContext.REFERENCE_TARGET, receiverExpression]
            if (targetDescriptor is CallableDescriptor) {
                receiverType = targetDescriptor.returnType
                receiver = targetDescriptor.toPsi() ?: return
            }
        }
        if (receiverType == null || !receiverType.isFunctionType) return
        val isExtension = receiverType.isExtensionFunctionType
        val shift = if (isExtension) 1 else 0
        val parameterIndex = parameterDescriptor.index - shift
        val newMode = if (parameterIndex >= 0)
            mode.withBehaviour(LambdaParameterInflowBehaviour(parameterIndex))
        else
            mode.withBehaviour(LambdaReceiverInflowBehaviour)
        receiver.passToProcessor(newMode)
    }

    private fun processDereferenceIfNeeded(
        expression: KtExpression,
        pseudoValue: PseudoValue,
        instr: InstructionWithReceivers
    ) {
        if (!parentUsage.params.showInstanceDereferences) return

        val receiver = instr.receiverValues[pseudoValue]
        val resolvedCall = when (instr) {
            is CallInstruction -> instr.resolvedCall
            is ReadValueInstruction -> (instr.target as? AccessTarget.Call)?.resolvedCall
            else -> null
        } ?: return

        if (receiver != null && resolvedCall.dispatchReceiver == receiver) {
            processor.process(KotlinSliceDereferenceUsage(expression, parentUsage, mode))
        }
    }

    private fun KtExpression.processPseudocodeUsages(processor: (PseudoValue, Instruction) -> Unit) {
        val pseudocode = pseudocodeCache[this] ?: return
        val pseudoValue = pseudocode.getElementValue(this) ?: return
        pseudocode.getUsages(pseudoValue).forEach { processor(pseudoValue, it) }
    }

    private fun KtExpression.processDereferences() {
        processPseudocodeUsages { pseudoValue, instr ->
            when (instr) {
                is ReadValueInstruction -> processDereferenceIfNeeded(this, pseudoValue, instr)
                is CallInstruction -> processDereferenceIfNeeded(this, pseudoValue, instr)
            }
        }
    }
}
