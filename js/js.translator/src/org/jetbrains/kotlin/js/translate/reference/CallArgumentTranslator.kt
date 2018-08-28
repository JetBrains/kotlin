/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.reference

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TemporaryConstVariable
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.expression.PatternTranslator
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.ArrayFIF
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.js.translate.utils.getReferenceToJsClass
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class CallArgumentTranslator private constructor(
        private val resolvedCall: ResolvedCall<*>,
        private val receiver: JsExpression?,
        private val context: TranslationContext
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
        var concatArguments: MutableList<JsExpression>? = null
        val argsToJsExpr = translateUnresolvedArguments(context(), resolvedCall)
        var varargPrimitiveType: PrimitiveType? = null

        for (parameterDescriptor in valueParameters) {
            val actualArgument = valueArgumentsByIndex[parameterDescriptor.index]

            if (actualArgument is VarargValueArgument) {

                val arguments = actualArgument.getArguments()

                if (!hasSpreadOperator) {
                    hasSpreadOperator = arguments.any { it.getSpreadElement() != null }
                }

                val varargElementType = parameterDescriptor.original.varargElementType!!
                varargPrimitiveType = KotlinBuiltIns.getPrimitiveType(varargElementType).takeUnless { varargElementType.isMarkedNullable }

                if (hasSpreadOperator) {
                    if (isNativeFunctionCall) {
                        argsBeforeVararg = result
                        result = mutableListOf()
                        concatArguments = prepareConcatArguments(arguments,
                                                                 translateResolvedArgument(actualArgument, argsToJsExpr),
                                                                 null)
                    }
                    else {
                        result.addAll(translateVarargArgument(actualArgument,
                                                              argsToJsExpr,
                                                              actualArgument.arguments.size > 1,
                                                              varargPrimitiveType))
                    }
                }
                else {
                    if (isNativeFunctionCall) {
                        result.addAll(translateResolvedArgument(actualArgument, argsToJsExpr))
                    }
                    else {
                        result.addAll(translateVarargArgument(actualArgument, argsToJsExpr, true, varargPrimitiveType))
                    }
                }
            }
            else {
                result.addAll(translateResolvedArgument(actualArgument, argsToJsExpr))
            }
        }

        if (isNativeFunctionCall && hasSpreadOperator) {
            assert(argsBeforeVararg != null) { "argsBeforeVararg should not be null" }
            assert(concatArguments != null) { "concatArguments should not be null" }

            if (!result.isEmpty()) {
                concatArguments!!.add(toArray(null, result))
            }

            if (!argsBeforeVararg!!.isEmpty()) {
                concatArguments!!.add(0, toArray(null, argsBeforeVararg))
            }

            result = mutableListOf(concatArgumentsIfNeeded(concatArguments!!, varargPrimitiveType, true))

            if (receiver != null) {
                cachedReceiver = context().getOrDeclareTemporaryConstVariable(receiver)
                result.add(0, cachedReceiver.reference())
            }
            else if (DescriptorUtils.isObject(resolvedCall.resultingDescriptor.containingDeclaration)) {
                cachedReceiver = context().getOrDeclareTemporaryConstVariable(
                        ReferenceTranslator.translateAsValueReference(resolvedCall.resultingDescriptor.containingDeclaration, context()))
                result.add(0, cachedReceiver.reference())
            }
            else {
                result.add(0, JsNullLiteral())
            }
        }

        val callableDescriptor = resolvedCall.resultingDescriptor
        if (callableDescriptor is FunctionDescriptor && callableDescriptor.isSuspend) {
            result.add(TranslationUtils.translateContinuationArgument(context()))
        }

        removeLastUndefinedArguments(result)

        return ArgumentsInfo(result, hasSpreadOperator, cachedReceiver)
    }

    private fun translateUnresolvedArguments(
            context: TranslationContext,
            resolvedCall: ResolvedCall<*>
    ): Map<ValueArgument, JsExpression> {
        val argsToParameters = resolvedCall.valueArguments
                .flatMap { (param, args) -> args.arguments.map { param to it } }
                .associate { (param, arg) -> arg to param }

        val argumentContexts = resolvedCall.call.valueArguments.associate { it to context.innerBlock() }

        var result = resolvedCall.call.valueArguments.associate { arg ->
            val argumentContext = argumentContexts[arg]!!
            val parenthisedArgumentExpression = arg.getArgumentExpression()

            val param = argsToParameters[arg]!!.original
            val isLambda = resolvedCall.resultingDescriptor.let { it.getFunctionalClassKind() != null || it is FunctionInvokeDescriptor }
            val parameterType = if (!isLambda) param.varargElementType ?: param.type else context.currentModule.builtIns.anyType

            var argJs = Translation.translateAsExpression(parenthisedArgumentExpression!!, argumentContext)
            if (!param.isVararg || arg.getSpreadElement() == null) {
                argJs = TranslationUtils.coerce(context, argJs, parameterType)
            }

            arg to argJs
        }

        val resolvedOrder = resolvedCall.valueArgumentsByIndex.orEmpty()
                .flatMap { it.arguments }
                .withIndex()
                .associate { (index, arg) -> arg to index }
        val argumentsAreOrdered = resolvedCall.call.valueArguments.withIndex().none { (index, arg) -> resolvedOrder[arg] != index }

        if (argumentContexts.values.any { !it.currentBlockIsEmpty() } || !argumentsAreOrdered) {
            result = result.map { (arg, expr) ->
                val argumentContext = argumentContexts[arg]!!
                arg to argumentContext.cacheExpressionIfNeeded(expr)
            }.toMap()
        }

        argumentContexts.values.forEach {
            context.moveVarsFrom(it)
            context.addStatementsToCurrentBlockFrom(it)
        }

        return result
    }

    private fun translateVarargArgument(
            resolvedArgument: ResolvedValueArgument,
            translatedArgs: Map<ValueArgument, JsExpression>,
            shouldWrapVarargInArray: Boolean,
            varargPrimitiveType: PrimitiveType?
    ): List<JsExpression> {
        val arguments = resolvedArgument.arguments
        if (arguments.isEmpty()) {
            return if (shouldWrapVarargInArray) {
                return listOf(toArray(varargPrimitiveType, listOf()))
            }
            else {
                listOf()
            }
        }

        val list = translateResolvedArgument(resolvedArgument, translatedArgs)

        return if (shouldWrapVarargInArray) {
            val concatArguments = prepareConcatArguments(arguments, list, varargPrimitiveType)
            val concatExpression = concatArgumentsIfNeeded(concatArguments, varargPrimitiveType, false)
            listOf(concatExpression)
        }
        else {
            listOf(JsAstUtils.invokeMethod(list[0], "slice"))
        }
    }

    private fun toArray(varargPrimitiveType: PrimitiveType?, elements: List<JsExpression>): JsExpression {
        return ArrayFIF.castOrCreatePrimitiveArray(context(),
                                                   varargPrimitiveType,
                                                   JsArrayLiteral(elements).apply { sideEffects = SideEffectKind.PURE })
    }

    private fun prepareConcatArguments(
            arguments: List<ValueArgument>,
            list: List<JsExpression>,
            varargPrimitiveType: PrimitiveType?
    ): MutableList<JsExpression> {
        assert(arguments.isNotEmpty()) { "arguments.size should not be 0" }
        assert(arguments.size == list.size) { "arguments.size: " + arguments.size + " != list.size: " + list.size }

        val concatArguments = mutableListOf<JsExpression>()
        var lastArrayContent = mutableListOf<JsExpression>()

        val size = arguments.size
        for (index in 0 until size) {
            val valueArgument = arguments[index]
            val expressionArgument = list[index]

            if (valueArgument.getSpreadElement() != null) {
                if (lastArrayContent.size > 0) {
                    concatArguments.add(toArray(varargPrimitiveType, lastArrayContent))
                    lastArrayContent = mutableListOf()
                }
                concatArguments.add(expressionArgument)
            }
            else {
                lastArrayContent.add(expressionArgument)
            }
        }
        if (lastArrayContent.size > 0) {
            concatArguments.add(toArray(varargPrimitiveType, lastArrayContent))
        }

        return concatArguments
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

        private fun translateResolvedArgument(
                resolvedArgument: ResolvedValueArgument,
                translatedArgs: Map<ValueArgument, JsExpression>
        ): List<JsExpression> {
            if (resolvedArgument is DefaultValueArgument) return listOf(Namer.getUndefinedExpression())
            return resolvedArgument.arguments.map { translatedArgs[it]!! }
        }

        private fun concatArgumentsIfNeeded(
                concatArguments: List<JsExpression>,
                varargPrimitiveType: PrimitiveType?,
                isMixed: Boolean
        ): JsExpression {
            assert(concatArguments.isNotEmpty()) { "concatArguments.size should not be 0" }

            return if (concatArguments.size > 1) {
                if (varargPrimitiveType != null) {
                    val method = if (isMixed) "arrayConcat" else "primitiveArrayConcat"
                    JsAstUtils.invokeKotlinFunction(method, concatArguments[0],
                                                    *concatArguments.subList(1, concatArguments.size).toTypedArray())
                }
                else {
                    JsInvocation(JsNameRef("concat", concatArguments[0]), concatArguments.subList(1, concatArguments.size))
                }
            }
            else {
                concatArguments[0]
            }
        }
    }
}

fun Map<TypeParameterDescriptor, KotlinType>.buildReifiedTypeArgs(context: TranslationContext): List<JsExpression> {
    val reifiedTypeArguments = mutableListOf<JsExpression>()
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
