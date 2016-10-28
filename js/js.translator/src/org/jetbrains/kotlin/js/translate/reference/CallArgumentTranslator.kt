/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.translate.reference

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.SideEffectKind
import com.google.dart.compiler.backend.js.ast.metadata.sideEffects
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TemporaryConstVariable
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.expression.LiteralFunctionTranslator
import org.jetbrains.kotlin.js.translate.expression.PatternTranslator
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.js.translate.utils.getReferenceToJsClass
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class CallArgumentTranslator private constructor(
        private val resolvedCall: ResolvedCall<*>,
        private val receiver: JsExpression?,
        context: TranslationContext
) : AbstractTranslator(context) {

    data class ArgumentsInfo(
            val valueArguments: List<JsExpression>,
            val hasSpreadOperator: Boolean,
            val cachedReceiver: TemporaryConstVariable?,
            val reifiedArguments: List<JsExpression> = listOf()
    ) {
        val translateArguments: List<JsExpression>
            get() = reifiedArguments + valueArguments
    }

    private val isNativeFunctionCall = AnnotationsUtils.isNativeObject(resolvedCall.candidateDescriptor)

    private fun removeLastUndefinedArguments(result: MutableList<JsExpression>) {
        var i = result.lastIndex

        while (i >= 0) {
            if (!JsAstUtils.isUndefinedExpression(result[i])) {
                break
            }
            i--
        }

        result.subList(i + 1, result.size).clear()
    }

    private fun translate(): ArgumentsInfo {
        val valueParameters = resolvedCall.resultingDescriptor.valueParameters
        var hasSpreadOperator = false
        var cachedReceiver: TemporaryConstVariable? = null

        var result: MutableList<JsExpression> = ArrayList(valueParameters.size)
        val valueArgumentsByIndex = resolvedCall.valueArgumentsByIndex ?: throw IllegalStateException(
                "Failed to arrange value arguments by index: " + resolvedCall.resultingDescriptor)
        var argsBeforeVararg: List<JsExpression>? = null
        var argumentsShouldBeExtractedToTmpVars = false
        val argContexts = SmartList<TranslationContext>()
        var concatArguments: MutableList<JsExpression>? = null

        for (parameterDescriptor in valueParameters) {
            val actualArgument = valueArgumentsByIndex[parameterDescriptor.index]

            val argContext = context().innerBlock()

            if (actualArgument is VarargValueArgument) {

                val arguments = actualArgument.getArguments()

                val size = arguments.size
                if (!hasSpreadOperator) {
                    hasSpreadOperator = arguments.any { it.getSpreadElement() != null }
                }

                if (hasSpreadOperator) {
                    if (isNativeFunctionCall) {
                        argsBeforeVararg = result
                        result = SmartList<JsExpression>()
                        val list = SmartList<JsExpression>()
                        translateValueArguments(arguments, list, argContext)
                        concatArguments = prepareConcatArguments(arguments, list)
                    }
                    else {
                        translateVarargArgument(arguments, result, argContext, size > 1)
                    }
                }
                else {
                    if (isNativeFunctionCall) {
                        translateValueArguments(arguments, result, argContext)
                    }
                    else {
                        translateVarargArgument(arguments, result, argContext, true)
                    }
                }
            }
            else {
                translateSingleArgument(parameterDescriptor, actualArgument, result, argContext)
            }

            context().moveVarsFrom(argContext)
            argContexts.add(argContext)
            argumentsShouldBeExtractedToTmpVars = argumentsShouldBeExtractedToTmpVars || !argContext.currentBlockIsEmpty()
        }

        if (argumentsShouldBeExtractedToTmpVars) {
            extractArguments(result, argContexts, context())
        }

        if (isNativeFunctionCall && hasSpreadOperator) {
            assert(argsBeforeVararg != null) { "argsBeforeVararg should not be null" }
            assert(concatArguments != null) { "concatArguments should not be null" }

            concatArguments!!.addAll(result)

            if (!argsBeforeVararg!!.isEmpty()) {
                concatArguments.add(0, JsArrayLiteral(argsBeforeVararg).apply { sideEffects = SideEffectKind.DEPENDS_ON_STATE })
            }

            result = SmartList(concatArgumentsIfNeeded(concatArguments))

            if (receiver != null) {
                cachedReceiver = context().getOrDeclareTemporaryConstVariable(receiver)
                result.add(0, cachedReceiver.reference())
            }
            else {
                result.add(0, JsLiteral.NULL)
            }
        }

        val callableDescriptor = resolvedCall.resultingDescriptor
        if (callableDescriptor is FunctionDescriptor && callableDescriptor.isSuspend &&
            callableDescriptor.initialSignatureDescriptor != null
        ) {
            val coroutineDescriptor = resolvedCall.getImplicitReceiverValue()!!.declarationDescriptor
            result.add(context().getAliasForDescriptor(coroutineDescriptor) ?: JsLiteral.THIS)
        }

        removeLastUndefinedArguments(result)

        return ArgumentsInfo(result, hasSpreadOperator, cachedReceiver)
    }

    companion object {

        @JvmStatic fun translate(resolvedCall: ResolvedCall<*>, receiver: JsExpression?, context: TranslationContext): ArgumentsInfo {
            return translate(resolvedCall, receiver, context, context.dynamicContext().jsBlock())
        }

        @JvmStatic fun translate(resolvedCall: ResolvedCall<*>, receiver: JsExpression?, context: TranslationContext,
                                 block: JsBlock): ArgumentsInfo {
            val innerContext = context.innerBlock(block)
            val argumentTranslator = CallArgumentTranslator(resolvedCall, receiver, innerContext)
            val result = argumentTranslator.translate()
            context.moveVarsFrom(innerContext)
            val callDescriptor = resolvedCall.candidateDescriptor

            if (CallExpressionTranslator.shouldBeInlined(callDescriptor)) {
                val typeArgs = resolvedCall.typeArguments
                return result.copy(reifiedArguments = typeArgs.buildReifiedTypeArgs(context))
            }

            return result
        }

        private fun translateSingleArgument(
                parameterDescriptor: ValueParameterDescriptor,
                actualArgument: ResolvedValueArgument,
                result: MutableList<JsExpression>,
                context: TranslationContext
        ) {
            val valueArguments = actualArgument.arguments

            if (actualArgument is DefaultValueArgument) {
                result += Namer.getUndefinedExpression()
                return
            }

            assert(actualArgument is ExpressionValueArgument)
            assert(valueArguments.size == 1)

            val argumentExpression = KtPsiUtil.deparenthesize(valueArguments[0].getArgumentExpression())!!

            result += if (parameterDescriptor.isCoroutine && argumentExpression is KtLambdaExpression) {
                val continuationType = parameterDescriptor.type.arguments.last().type
                val continuationDescriptor = continuationType.constructor.declarationDescriptor as ClassDescriptor
                val controllerType = parameterDescriptor.type.arguments[0].type
                val controllerDescriptor = controllerType.constructor.declarationDescriptor as ClassDescriptor
                LiteralFunctionTranslator(context).translate(
                        argumentExpression.functionLiteral, continuationDescriptor, controllerDescriptor)
            }
            else {
                Translation.translateAsExpression(argumentExpression, context)
            }
        }

        private fun translateVarargArgument(arguments: List<ValueArgument>, result: MutableList<JsExpression>,
                                            context: TranslationContext, shouldWrapVarargInArray: Boolean) {
            if (arguments.isEmpty()) {
                if (shouldWrapVarargInArray) {
                    result.add(JsArrayLiteral(listOf<JsExpression>()).apply { sideEffects = SideEffectKind.DEPENDS_ON_STATE })
                }
                return
            }

            val list: MutableList<JsExpression> = if (shouldWrapVarargInArray) {
                if (arguments.size == 1) SmartList<JsExpression>() else ArrayList<JsExpression>(arguments.size)
            }
            else {
                result
            }

            translateValueArguments(arguments, list, context)

            if (shouldWrapVarargInArray) {
                val concatArguments = prepareConcatArguments(arguments, list)
                val concatExpression = concatArgumentsIfNeeded(concatArguments)
                result.add(concatExpression)
            }
            else if (result.size == 1) {
                result[0] = JsAstUtils.invokeMethod(result[0], "slice")
            }
        }

        private fun translateValueArguments(arguments: List<ValueArgument>, list: MutableList<JsExpression>,
                                            context: TranslationContext) {
            val argContexts = SmartList<TranslationContext>()
            var argumentsShouldBeExtractedToTmpVars = false
            for (argument in arguments) {
                val argumentExpression = argument.getArgumentExpression()!!
                val argContext = context.innerBlock()
                val argExpression = Translation.translateAsExpression(argumentExpression, argContext)
                list.add(argExpression)
                context.moveVarsFrom(argContext)
                argContexts.add(argContext)
                argumentsShouldBeExtractedToTmpVars = argumentsShouldBeExtractedToTmpVars || !argContext.currentBlockIsEmpty()
            }
            if (argumentsShouldBeExtractedToTmpVars) {
                extractArguments(list, argContexts, context)
            }
        }

        private fun concatArgumentsIfNeeded(concatArguments: List<JsExpression>): JsExpression {
            assert(concatArguments.isNotEmpty()) { "concatArguments.size should not be 0" }

            if (concatArguments.size > 1) {
                return JsInvocation(JsNameRef("concat", concatArguments[0]), concatArguments.subList(1, concatArguments.size))

            }
            else {
                return concatArguments[0]
            }
        }

        private fun prepareConcatArguments(arguments: List<ValueArgument>, list: List<JsExpression>): MutableList<JsExpression> {
            assert(arguments.isNotEmpty()) { "arguments.size should not be 0" }
            assert(arguments.size == list.size) { "arguments.size: " + arguments.size + " != list.size: " + list.size }

            val concatArguments = SmartList<JsExpression>()
            var lastArrayContent: MutableList<JsExpression> = SmartList()

            val size = arguments.size
            for (index in 0..size - 1) {
                val valueArgument = arguments[index]
                val expressionArgument = list[index]

                if (valueArgument.getSpreadElement() != null) {
                    if (lastArrayContent.size > 0) {
                        concatArguments.add(JsArrayLiteral(lastArrayContent).apply { sideEffects = SideEffectKind.DEPENDS_ON_STATE })
                        concatArguments.add(expressionArgument)
                        lastArrayContent = SmartList<JsExpression>()
                    }
                    else {
                        concatArguments.add(expressionArgument)
                    }
                }
                else {
                    lastArrayContent.add(expressionArgument)
                }
            }
            if (lastArrayContent.size > 0) {
                concatArguments.add(JsArrayLiteral(lastArrayContent).apply { sideEffects = SideEffectKind.DEPENDS_ON_STATE })
            }

            return concatArguments
        }

        private fun extractArguments(argExpressions: MutableList<JsExpression>, argContexts: List<TranslationContext>,
                                     context: TranslationContext) {
            for (i in argExpressions.indices) {
                val argContext = argContexts[i]
                val jsArgExpression = argExpressions[i]
                if (argContext.currentBlockIsEmpty() && TranslationUtils.isCacheNeeded(jsArgExpression)) {
                    argExpressions[i] = context.defineTemporary(jsArgExpression)
                }
                else {
                    context.addStatementsToCurrentBlockFrom(argContext)
                }
            }
        }
    }

}

public fun Map<TypeParameterDescriptor, KotlinType>.buildReifiedTypeArgs(
        context: TranslationContext
): List<JsExpression> {

    val reifiedTypeArguments = SmartList<JsExpression>()
    val patternTranslator = PatternTranslator.newInstance(context)

    for (param in keys.sortedBy { it.index }) {
        if (!param.isReified) continue

        val argumentType = get(param) ?: continue

        reifiedTypeArguments.add(getReferenceToJsClass(argumentType, context))

        val isCheckCallable = patternTranslator.getIsTypeCheckCallable(argumentType)
        reifiedTypeArguments.add(isCheckCallable)
    }

    return reifiedTypeArguments
}
