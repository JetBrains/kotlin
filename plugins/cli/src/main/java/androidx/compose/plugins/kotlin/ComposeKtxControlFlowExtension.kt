/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import com.intellij.util.SmartFMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.ControlFlowBuilder
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.AccessTarget
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.InstructionWithValue
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicKind
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.extensions.KtxControlFlowExtension
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtStatementExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.ValueArgument
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.getFakeDescriptorForObject
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import java.util.ArrayList

class ComposeKtxControlFlowExtension : KtxControlFlowExtension {

    private class ControlFlowVisitor(
        val builder: ControlFlowBuilder,
        val visitor: KtVisitorVoid,
        val trace: BindingTrace
    ) {

        private fun mark(element: KtElement) = builder.mark(element)

        private fun createSyntheticValue(
            instructionElement: KtElement,
            kind: MagicKind,
            vararg from: KtElement
        ): PseudoValue =
            builder.magic(
                instructionElement,
                null,
                elementsToValues(from.asList()), kind
            ).outputValue

        private fun createNonSyntheticValue(
            to: KtElement,
            from: List<KtElement?>,
            kind: MagicKind
        ): PseudoValue =
            builder.magic(to, to, elementsToValues(from), kind).outputValue

        private fun getBoundOrUnreachableValue(element: KtElement?): PseudoValue? {
            if (element == null) return null

            val value = builder.getBoundValue(element)
            return if (value != null || element is KtDeclaration) value
            else builder.newValue(element)
        }

        private fun elementsToValues(from: List<KtElement?>): List<PseudoValue> =
            from.mapNotNull { element -> getBoundOrUnreachableValue(element) }

        private fun instructionsToValues(from: List<InstructionWithValue>): List<PseudoValue> =
            from.mapNotNull { instruction -> instruction.outputValue }

        private fun generateInstructions(element: KtElement?) {
            if (element == null) return
            element.accept(visitor)
            checkNothingType(element)
        }

        private fun checkNothingType(element: KtElement) {
            if (element !is KtExpression) return

            val expression = KtPsiUtil.deparenthesize(element) ?: return

            if (expression is KtStatementExpression || expression is KtTryExpression ||
                expression is KtIfExpression || expression is KtWhenExpression
            ) {
                return
            }

            val type = trace.bindingContext.getType(expression)
            if (type != null && KotlinBuiltIns.isNothing(type)) {
                builder.jumpToError(expression)
            }
        }

        private fun checkAndGenerateCall(resolvedCall: ResolvedCall<*>?): Boolean {
            if (resolvedCall == null) return false
            generateCall(resolvedCall)
            return true
        }

        private fun generateCall(resolvedCall: ResolvedCall<*>): InstructionWithValue {
            val callElement = resolvedCall.call.callElement

            val receivers = getReceiverValues(resolvedCall)

            var parameterValues = SmartFMap.emptyMap<PseudoValue, ValueParameterDescriptor>()
            for (argument in resolvedCall.call.valueArguments) {
                val argumentMapping = resolvedCall.getArgumentMapping(argument)
                val argumentExpression = argument.getArgumentExpression()
                if (argumentMapping is ArgumentMatch) {
                    parameterValues = generateValueArgument(
                        argument, argumentMapping.valueParameter,
                        parameterValues
                    )
                } else if (argumentExpression != null) {
                    generateInstructions(argumentExpression)
                    createSyntheticValue(
                        argumentExpression,
                        MagicKind.VALUE_CONSUMER,
                        argumentExpression
                    )
                }
            }

            if (resolvedCall.resultingDescriptor is VariableDescriptor) {
                // If a callee of the call is just a variable (without 'invoke'), 'read variable' is generated.
                // todo : process arguments for such a case (KT-5387)
                val callExpression = callElement as? KtExpression
                    ?: error("Variable-based call without callee expression: " + callElement.text)
                assert(parameterValues.isEmpty()) {
                    "Variable-based call with non-empty argument list: " + callElement.text
                }
                return builder.readVariable(callExpression, resolvedCall, receivers)
            }

            mark(resolvedCall.call.callElement)
            return builder.call(callElement, resolvedCall, receivers, parameterValues)
        }

        private fun getReceiverValues(
            resolvedCall: ResolvedCall<*>
        ): Map<PseudoValue, ReceiverValue> {
            var varCallResult: PseudoValue? = null
            var explicitReceiver: ReceiverValue? = null
            if (resolvedCall is VariableAsFunctionResolvedCall) {
                varCallResult = generateCall(resolvedCall.variableCall).outputValue

                val kind = resolvedCall.explicitReceiverKind
                //noinspection EnumSwitchStatementWhichMissesCases
                when (kind) {
                    ExplicitReceiverKind.DISPATCH_RECEIVER ->
                        explicitReceiver = resolvedCall.dispatchReceiver
                    ExplicitReceiverKind.EXTENSION_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS ->
                        explicitReceiver = resolvedCall.extensionReceiver
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> Unit
                }
            }

            var receiverValues = SmartFMap.emptyMap<PseudoValue, ReceiverValue>()
            if (explicitReceiver != null && varCallResult != null) {
                receiverValues = receiverValues.plus(varCallResult, explicitReceiver)
            }
            val callElement = resolvedCall.call.callElement
            receiverValues = getReceiverValues(
                callElement,
                resolvedCall.dispatchReceiver,
                receiverValues
            )
            receiverValues = getReceiverValues(
                callElement,
                resolvedCall.extensionReceiver,
                receiverValues
            )
            return receiverValues
        }

        private fun getReceiverValues(
            callElement: KtElement,
            receiver: ReceiverValue?,
            receiverValuesArg: SmartFMap<PseudoValue, ReceiverValue>
        ): SmartFMap<PseudoValue, ReceiverValue> {
            var receiverValues = receiverValuesArg
            if (receiver == null || receiverValues.containsValue(receiver)) return receiverValues

            when (receiver) {
                is ImplicitReceiver -> {
                    if (callElement is KtCallExpression) {
                        val declaration = receiver.declarationDescriptor
                        if (declaration is ClassDescriptor) {
                            val fakeDescriptor = getFakeDescriptorForObject(declaration)
                            val calleeExpression = callElement.calleeExpression
                            if (fakeDescriptor != null && calleeExpression != null) {
                                builder.read(
                                    calleeExpression,
                                    AccessTarget.Declaration(fakeDescriptor),
                                    emptyMap()
                                )
                            }
                        }
                    }
                    receiverValues = receiverValues.plus(
                        createSyntheticValue(callElement, MagicKind.IMPLICIT_RECEIVER),
                        receiver
                    )
                }
                is ExpressionReceiver -> {
                    val expression = receiver.expression
                    if (builder.getBoundValue(expression) == null) {
                        generateInstructions(expression)
                    }

                    val receiverPseudoValue = getBoundOrUnreachableValue(expression)
                    if (receiverPseudoValue != null) {
                        receiverValues = receiverValues.plus(receiverPseudoValue, receiver)
                    }
                }
                is TransientReceiver -> {
                    // Do nothing
                }
                else -> {
                    throw IllegalArgumentException("Unknown receiver kind: $receiver")
                }
            }

            return receiverValues
        }

        private fun generateValueArgument(
            valueArgument: ValueArgument,
            parameterDescriptor: ValueParameterDescriptor,
            parameterValuesArg: SmartFMap<PseudoValue, ValueParameterDescriptor>
        ): SmartFMap<PseudoValue, ValueParameterDescriptor> {
            var parameterValues = parameterValuesArg
            val expression = valueArgument.getArgumentExpression()
            if (expression != null) {
                if (!valueArgument.isExternal()) {
                    generateInstructions(expression)
                }

                val argValue = getBoundOrUnreachableValue(expression)
                if (argValue != null) {
                    parameterValues = parameterValues.plus(argValue, parameterDescriptor)
                }
            }
            return parameterValues
        }

        private fun copyValue(from: KtElement?, to: KtElement) {
            getBoundOrUnreachableValue(from)?.let { builder.bindValue(it, to) }
        }

        fun visitComposerCallInfo(
            memoize: ComposerCallInfo,
            inputInstructions: MutableList<InstructionWithValue>
        ) {
            memoize.ctorCall?.let { inputInstructions.add(generateCall(it)) }
            memoize.composerCall?.let { inputInstructions.add(generateCall(it)) }
            memoize.validations.forEach { validation ->
                validation.validationCall?.let { inputInstructions.add(generateCall(it)) }
                validation.assignment?.let { inputInstructions.add(generateCall(it)) }
            }
        }

        fun visitKtxElement(element: KtxElement) {
            val inputExpressions = ArrayList<KtElement>()
            val inputInstructions = ArrayList<InstructionWithValue>()
            val tagExpr = element.simpleTagName ?: element.qualifiedTagName ?: return

            inputExpressions.add(tagExpr)

            for (attribute in element.attributes) {
                val valueExpr = attribute.value ?: attribute.key ?: break
                generateInstructions(valueExpr)
                mark(valueExpr)
                inputExpressions.add(valueExpr)
            }

            element.bodyLambdaExpression?.let {
                generateInstructions(it)
                inputExpressions.add(it)
            }

            val elementCall = trace.get(ComposeWritableSlices.RESOLVED_KTX_CALL, element) ?: return
            var node: EmitOrCallNode? = elementCall.emitOrCall

            elementCall.getComposerCall?.let { inputInstructions.add(generateCall(it)) }

            while (node != null) {
                when (node) {
                    is MemoizedCallNode -> {
                        visitComposerCallInfo(node.memoize, inputInstructions)
                        node = node.call
                    }
                    is NonMemoizedCallNode -> {
                        inputInstructions.add(generateCall(node.resolvedCall))
                        node = node.nextCall
                    }
                    is EmitCallNode -> {
                        visitComposerCallInfo(node.memoize, inputInstructions)
                        node = null
                    }
                    is ErrorNode -> {
                        node = null
                    }
                }
            }

            builder.magic(
                instructionElement = element,
                valueElement = null,
                inputValues = elementsToValues(inputExpressions) +
                        instructionsToValues(inputInstructions),
                kind = MagicKind.VALUE_CONSUMER
            )
        }
    }

    override fun visitKtxElement(
        element: KtxElement,
        builder: ControlFlowBuilder,
        visitor: KtVisitorVoid,
        trace: BindingTrace
    ) {
        ControlFlowVisitor(
            builder,
            visitor,
            trace
        ).visitKtxElement(element)
    }
}