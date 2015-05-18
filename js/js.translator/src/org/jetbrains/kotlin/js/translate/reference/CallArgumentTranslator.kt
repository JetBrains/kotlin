/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.js.translate.context.TemporaryConstVariable
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.expression.PatternTranslator
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.JetType

import java.util.ArrayList
import java.util.Collections
import kotlin.platform.platformStatic

public class CallArgumentTranslator private (
        private val resolvedCall: ResolvedCall<*>,
        private val receiver: JsExpression?,
        context: TranslationContext
) : AbstractTranslator(context) {

    public data class ArgumentsInfo(
            public val valueArguments: List<JsExpression>,
            public val hasSpreadOperator: Boolean,
            public val cachedReceiver: TemporaryConstVariable?,
            public val reifiedArguments: List<JsExpression> = listOf()
    ) {
        public val translateArguments: List<JsExpression>
            get() = reifiedArguments + valueArguments
    }

    private enum class ArgumentsKind {
        HAS_EMPTY_EXPRESSION_ARGUMENT,
        HAS_NOT_EMPTY_EXPRESSION_ARGUMENT
    }

    private val isNativeFunctionCall = AnnotationsUtils.isNativeObject(resolvedCall.getCandidateDescriptor())

    private fun removeLastUndefinedArguments(result: MutableList<JsExpression>) {
        var i = result.size() - 1

        while (i >= 0) {
            if (result.get(i) != context().namer().getUndefinedExpression()) {
                break
            }
            i--
        }

        result.subList(i + 1, result.size()).clear()
    }

    private fun translate(): ArgumentsInfo {
        val valueParameters = resolvedCall.getResultingDescriptor().getValueParameters()
        if (valueParameters.isEmpty()) {
            return ArgumentsInfo(listOf<JsExpression>(), false, null)
        }
        var hasSpreadOperator = false
        var cachedReceiver: TemporaryConstVariable? = null

        var result: MutableList<JsExpression> = ArrayList(valueParameters.size())
        val valueArgumentsByIndex = resolvedCall.getValueArgumentsByIndex()
        if (valueArgumentsByIndex == null) {
            throw IllegalStateException("Failed to arrange value arguments by index: " + resolvedCall.getResultingDescriptor())
        }
        var argsBeforeVararg: List<JsExpression>? = null
        var argumentsShouldBeExtractedToTmpVars = false
        val argContexts = SmartList<TranslationContext>()
        var kind = ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT
        var concatArguments: MutableList<JsExpression>? = null

        for (parameterDescriptor in valueParameters) {
            val actualArgument = valueArgumentsByIndex.get(parameterDescriptor.getIndex())

            val argContext = context().innerBlock()

            if (actualArgument is VarargValueArgument) {

                val arguments = actualArgument.getArguments()

                val size = arguments.size()
                var i = 0
                while (i != size) {
                    if (arguments.get(i).getSpreadElement() != null) {
                        hasSpreadOperator = true
                        break
                    }
                    ++i
                }

                if (hasSpreadOperator) {
                    if (isNativeFunctionCall) {
                        argsBeforeVararg = result
                        result = SmartList<JsExpression>()
                        val list = SmartList<JsExpression>()
                        kind = translateValueArguments(arguments, list, argContext)
                        concatArguments = prepareConcatArguments(arguments, list)
                    }
                    else {
                        kind = translateVarargArgument(arguments, result, argContext, size > 1)
                    }
                }
                else {
                    kind = translateVarargArgument(arguments, result, argContext, !isNativeFunctionCall)
                }
            }
            else {
                kind = translateSingleArgument(actualArgument, result, argContext)
            }

            context().moveVarsFrom(argContext)
            argContexts.add(argContext)
            argumentsShouldBeExtractedToTmpVars = argumentsShouldBeExtractedToTmpVars || !argContext.currentBlockIsEmpty()

            if (kind == ArgumentsKind.HAS_EMPTY_EXPRESSION_ARGUMENT) break
        }

        if (argumentsShouldBeExtractedToTmpVars) {
            extractArguments(result, argContexts, context(), kind == ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT)
        }

        if (isNativeFunctionCall && hasSpreadOperator) {
            assert(argsBeforeVararg != null, "argsBeforeVararg should not be null")
            assert(concatArguments != null, "concatArguments should not be null")

            concatArguments!!.addAll(result)

            if (!argsBeforeVararg!!.isEmpty()) {
                concatArguments!!.add(0, JsArrayLiteral(argsBeforeVararg))
            }

            result = SmartList(concatArgumentsIfNeeded(concatArguments!!))

            if (receiver != null) {
                cachedReceiver = context().getOrDeclareTemporaryConstVariable(receiver)
                result.add(0, cachedReceiver!!.reference())
            }
            else {
                result.add(0, JsLiteral.NULL)
            }
        }

        removeLastUndefinedArguments(result)
        return ArgumentsInfo(result, hasSpreadOperator, cachedReceiver)
    }

    companion object {

        platformStatic
        public fun translate(resolvedCall: ResolvedCall<*>, receiver: JsExpression?, context: TranslationContext): ArgumentsInfo {
            return translate(resolvedCall, receiver, context, context.dynamicContext().jsBlock())
        }

        platformStatic
        public fun translate(resolvedCall: ResolvedCall<*>, receiver: JsExpression?, context: TranslationContext, block: JsBlock): ArgumentsInfo {
            val innerContext = context.innerBlock(block)
            val argumentTranslator = CallArgumentTranslator(resolvedCall, receiver, innerContext)
            val result = argumentTranslator.translate()
            context.moveVarsFrom(innerContext)
            val callDescriptor = resolvedCall.getCandidateDescriptor()

            if (CallExpressionTranslator.shouldBeInlined(callDescriptor)) {
                val typeArgs = resolvedCall.getTypeArguments()
                return typeArgs.addReifiedTypeArgsTo(result, context)
            }

            return result;
        }

        private fun translateSingleArgument(actualArgument: ResolvedValueArgument, result: MutableList<JsExpression>, context: TranslationContext): ArgumentsKind {
            val valueArguments = actualArgument.getArguments()

            if (actualArgument is DefaultValueArgument) {
                result.add(context.namer().getUndefinedExpression())
                return ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT
            }

            assert(actualArgument is ExpressionValueArgument)
            assert(valueArguments.size() == 1)

            val argumentExpression = valueArguments.get(0).getArgumentExpression()
            assert(argumentExpression != null)

            val jsExpression = Translation.translateAsExpression(argumentExpression, context)
            result.add(jsExpression)

            if (JsAstUtils.isEmptyExpression(jsExpression)) {
                return ArgumentsKind.HAS_EMPTY_EXPRESSION_ARGUMENT
            }
            else {
                return ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT
            }
        }

        private fun translateVarargArgument(arguments: List<ValueArgument>, result: MutableList<JsExpression>, context: TranslationContext, shouldWrapVarargInArray: Boolean): ArgumentsKind {
            if (arguments.isEmpty()) {
                if (shouldWrapVarargInArray) {
                    result.add(JsArrayLiteral(listOf<JsExpression>()))
                }
                return ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT
            }

            val list: MutableList<JsExpression>
            if (shouldWrapVarargInArray) {
                list = if (arguments.size() == 1) SmartList<JsExpression>() else ArrayList<JsExpression>(arguments.size())
            }
            else {
                list = result
            }

            val resultKind = translateValueArguments(arguments, list, context)

            if (shouldWrapVarargInArray) {
                val concatArguments = prepareConcatArguments(arguments, list)
                val concatExpression = concatArgumentsIfNeeded(concatArguments)
                result.add(concatExpression)
            }

            return resultKind
        }

        private fun translateValueArguments(arguments: List<ValueArgument>, list: MutableList<JsExpression>, context: TranslationContext): ArgumentsKind {
            var resultKind = ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT
            val argContexts = SmartList<TranslationContext>()
            var argumentsShouldBeExtractedToTmpVars = false
            for (argument in arguments) {
                val argumentExpression = argument.getArgumentExpression()
                assert(argumentExpression != null)
                val argContext = context.innerBlock()
                val argExpression = Translation.translateAsExpression(argumentExpression, argContext)
                list.add(argExpression)
                context.moveVarsFrom(argContext)
                argContexts.add(argContext)
                argumentsShouldBeExtractedToTmpVars = argumentsShouldBeExtractedToTmpVars || !argContext.currentBlockIsEmpty()
                if (JsAstUtils.isEmptyExpression(argExpression)) {
                    resultKind = ArgumentsKind.HAS_EMPTY_EXPRESSION_ARGUMENT
                    break
                }
            }
            if (argumentsShouldBeExtractedToTmpVars) {
                extractArguments(list, argContexts, context, resultKind == ArgumentsKind.HAS_NOT_EMPTY_EXPRESSION_ARGUMENT)
            }
            return resultKind
        }

        private fun concatArgumentsIfNeeded(concatArguments: List<JsExpression>): JsExpression {
            assert(concatArguments.size() > 0, "concatArguments.size should not be 0")

            if (concatArguments.size() > 1) {
                return JsInvocation(JsNameRef("concat", concatArguments.get(0)), concatArguments.subList(1, concatArguments.size()))

            }
            else {
                return concatArguments.get(0)
            }
        }

        private fun prepareConcatArguments(arguments: List<ValueArgument>, list: List<JsExpression>): MutableList<JsExpression> {
            assert(arguments.size() != 0, "arguments.size should not be 0")
            assert(arguments.size() == list.size()) { "arguments.size: " + arguments.size() + " != list.size: " + list.size() }

            val concatArguments = SmartList<JsExpression>()
            var lastArrayContent: MutableList<JsExpression> = SmartList()

            val size = arguments.size()
            for (index in 0..size - 1) {
                val valueArgument = arguments.get(index)
                val expressionArgument = list.get(index)

                if (valueArgument.getSpreadElement() != null) {
                    if (lastArrayContent.size() > 0) {
                        concatArguments.add(JsArrayLiteral(lastArrayContent))
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
            if (lastArrayContent.size() > 0) {
                concatArguments.add(JsArrayLiteral(lastArrayContent))
            }

            return concatArguments
        }

        private fun extractArguments(argExpressions: MutableList<JsExpression>, argContexts: List<TranslationContext>, context: TranslationContext, toTmpVars: Boolean) {
            for (i in argExpressions.indices) {
                val argContext = argContexts.get(i)
                val jsArgExpression = argExpressions.get(i)
                if (argContext.currentBlockIsEmpty() && TranslationUtils.isCacheNeeded(jsArgExpression)) {
                    if (toTmpVars) {
                        val temporaryVariable = context.declareTemporary(jsArgExpression)
                        context.addStatementToCurrentBlock(temporaryVariable.assignmentExpression().makeStmt())
                        argExpressions.set(i, temporaryVariable.reference())
                    }
                    else {
                        context.addStatementToCurrentBlock(jsArgExpression.makeStmt())
                    }
                }
                else {
                    context.addStatementsToCurrentBlockFrom(argContext)
                }
            }
        }
    }

}

private fun Map<TypeParameterDescriptor, JetType>.addReifiedTypeArgsTo(
        info: CallArgumentTranslator.ArgumentsInfo,
        context: TranslationContext
): CallArgumentTranslator.ArgumentsInfo {

    val reifiedTypeArguments = SmartList<JsExpression>()
    val patternTranslator = PatternTranslator.newInstance(context)

    for (param in keySet().sortBy { it.getIndex() }) {
        if (!param.isReified()) continue

        val argumentType = get(param)
        if (argumentType == null) continue

        val isCheckCallable = patternTranslator.getIsTypeCheckCallable(argumentType)
        reifiedTypeArguments.add(isCheckCallable)
    }

    return info.copy(reifiedArguments = reifiedTypeArguments)
}
