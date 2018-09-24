/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

@file:JvmName("LoopTranslator")

package org.jetbrains.kotlin.js.translate.expression

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.ArrayFIF
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.*
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getReceiverParameterForReceiver
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getLoopRange
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils.getFunctionByName
import org.jetbrains.kotlin.resolve.DescriptorUtils.getPropertyByName
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.KotlinType

fun createWhile(doWhile: Boolean, expression: KtWhileExpressionBase, context: TranslationContext): JsNode {
    val conditionExpression = expression.condition ?:
                              throw IllegalArgumentException("condition expression should not be null: ${expression.text}")
    val conditionBlock = JsBlock()
    var jsCondition = Translation.translateAsExpression(conditionExpression, context, conditionBlock)
    val body = expression.body
    var bodyStatement =
        if (body != null)
            Translation.translateAsStatementAndMergeInBlockIfNeeded(body, context)
        else
            JsEmpty

    if (!conditionBlock.isEmpty) {
        val breakIfConditionIsFalseStatement = JsIf(not(jsCondition), JsBreak().apply { source = expression })
                .apply { source = expression }
        val bodyBlock = convertToBlock(bodyStatement)
        jsCondition = JsBooleanLiteral(true)

        if (doWhile) {
            // translate to: tmpSecondRun = false;
            // do { if(tmpSecondRun) { <expr> if(!tmpExprVar) break; } else tmpSecondRun=true; <body> } while(true)
            val secondRun = context.defineTemporary(JsBooleanLiteral(false).source(expression))
            conditionBlock.statements.add(breakIfConditionIsFalseStatement)
            val ifStatement = JsIf(secondRun, conditionBlock, assignment(secondRun, JsBooleanLiteral(true)).source(expression).makeStmt())
            bodyBlock.statements.add(0, ifStatement.apply { source = expression })
        }
        else {
            conditionBlock.statements.add(breakIfConditionIsFalseStatement)
            bodyBlock.statements.addAll(0, conditionBlock.statements)
        }

        bodyStatement = bodyBlock
    }

    val result = if (doWhile) JsDoWhile() else JsWhile()
    result.condition = jsCondition
    result.body = bodyStatement
    return result.source(expression)
}

private val rangeToFunctionName = FqName("kotlin.Int.rangeTo")
private val untilFunctionName = FqName("kotlin.ranges.until")
private val downToFunctionName = FqName("kotlin.ranges.downTo")
private val stepFunctionName = FqName("kotlin.ranges.step")
private val intRangeName = FqName("kotlin.ranges.IntRange")
private val intProgressionName = FqName("kotlin.ranges.IntProgression")

private val withIndexFqName = FqName("kotlin.collections.withIndex")
private val sequenceWithIndexFqName = FqName("kotlin.sequences.withIndex")
private val indicesFqName = FqName("kotlin.collections.indices")

private val sequenceFqName = FqName("kotlin.sequences.Sequence")

fun translateForExpression(expression: KtForExpression, context: TranslationContext): JsStatement {
    val loopRange = getLoopRange(expression).let {
        val deparenthesized = KtPsiUtil.deparenthesize(it)!!
        if (deparenthesized is KtStringTemplateExpression) it else deparenthesized
    }
    val rangeType = getTypeForExpression(context.bindingContext(), loopRange)

    fun isForOverRange(): Boolean {
        //TODO: long range?
        val fqn = rangeType.constructor.declarationDescriptor?.fqNameSafe ?: return false
        return fqn == intRangeName
    }

    fun extractForOverRangeLiteral(): RangeLiteral? {
        val fqn = rangeType.constructor.declarationDescriptor?.fqNameSafe
        if (fqn != intRangeName && fqn != intProgressionName) return null

        var resolvedCall = loopRange.getResolvedCall(context.bindingContext()) ?: return null
        var step: KtExpression? = null
        if (resolvedCall.resultingDescriptor.fqNameSafe == stepFunctionName) {
            step = resolvedCall.call.valueArguments[0].getArgumentExpression() ?: return null
            resolvedCall = (resolvedCall.extensionReceiver as? ExpressionReceiver)?.expression?.getResolvedCall(context.bindingContext()) ?:
                           return null
        }

        val first = ((resolvedCall.extensionReceiver ?: resolvedCall.dispatchReceiver) as? ExpressionReceiver)?.expression ?: return null
        val second = resolvedCall.valueArgumentsByIndex?.firstOrNull()?.arguments?.firstOrNull()?.getArgumentExpression() ?: return null

        val type = when (resolvedCall.resultingDescriptor.fqNameSafe) {
            rangeToFunctionName -> RangeType.RANGE_TO
            untilFunctionName -> RangeType.UNTIL
            downToFunctionName -> RangeType.DOWN_TO
            else -> return null
        }

        return RangeLiteral(type, first, second, step)
    }

    fun isForOverArray(): Boolean {
        return KotlinBuiltIns.isArray(rangeType) || KotlinBuiltIns.isPrimitiveArray(rangeType)
    }


    val loopParameter = expression.loopParameter!!
    val destructuringParameter: KtDestructuringDeclaration? = loopParameter.destructuringDeclaration
    val parameterName = if (destructuringParameter == null) {
        context.getNameForElement(loopParameter)
    }
    else {
        JsScope.declareTemporary()
    }

    fun KtDeclaration.extractDescriptor() = context.bindingContext()[BindingContext.VARIABLE, this]?.takeUnless { it.name.isSpecial }

    fun extractWithIndexCall(): WithIndexInfo? {
        val resolvedCall = loopRange.getResolvedCall(context.bindingContext()) ?: return null
        val fqName = resolvedCall.resultingDescriptor.fqNameSafe
        val (indexDescriptor, elementDescriptor) = when (fqName) {
            withIndexFqName, sequenceWithIndexFqName -> {
                if (destructuringParameter == null) return null
                destructuringParameter.entries.let { Pair(it[0].extractDescriptor(), it[1].extractDescriptor()) }
            }
            indicesFqName -> {
                if (destructuringParameter != null) return null
                val varDescriptor = context.bindingContext()[BindingContext.DECLARATION_TO_DESCRIPTOR, loopParameter] as?
                                            VariableDescriptor ?: return null
                Pair(varDescriptor, null)
            }
            else -> return null
        }

        val receiverClass = resolvedCall.resultingDescriptor.extensionReceiverParameter?.type?.constructor?.declarationDescriptor as?
                                    ClassDescriptor ?: return null
        val receiverType = when {
            KotlinBuiltIns.isArrayOrPrimitiveArray(receiverClass) -> WithIndexReceiverType.ARRAY
            KotlinBuiltIns.isCollectionOrNullableCollection(receiverClass.defaultType) -> WithIndexReceiverType.COLLECTION
            KotlinBuiltIns.isIterableOrNullableIterable(receiverClass.defaultType) -> WithIndexReceiverType.ITERABLE
            receiverClass.fqNameSafe == sequenceFqName -> WithIndexReceiverType.SEQUENCE
            else -> return null
        }

        val receiver = resolvedCall.extensionReceiver ?: return null
        val arrayExpr = when (receiver) {
            is ExpressionReceiver -> Translation.translateAsExpression(receiver.expression, context)
            is ImplicitReceiver -> context.getDispatchReceiver(getReceiverParameterForReceiver(receiver))
            else -> return null
        }

        return WithIndexInfo(receiverType, indexDescriptor, elementDescriptor, arrayExpr)
    }

    fun translateBody(itemValue: JsExpression?): JsStatement? {
        val realBody = expression.body?.let { Translation.translateAsStatementAndMergeInBlockIfNeeded(it, context) }
        if (itemValue == null && destructuringParameter == null) {
            return realBody
        }
        else {
            val block = JsBlock()

            val currentVarInit =
                if (destructuringParameter == null) {
                    val loopParameterDescriptor = (getDescriptorForElement(context.bindingContext(), loopParameter) as CallableDescriptor)
                    val loopParameterType = loopParameterDescriptor.returnType ?: context.currentModule.builtIns.anyType
                    val coercedItemValue = itemValue?.let { TranslationUtils.coerce(context, it, loopParameterType) }
                    newVar(parameterName, coercedItemValue).apply { source = expression.loopRange }
                }
                else {
                    val innerBlockContext = context.innerBlock(block)
                    if (itemValue != null) {
                        val parameterStatement = JsAstUtils.newVar(parameterName, itemValue).apply { source = expression.loopRange }
                        innerBlockContext.addStatementToCurrentBlock(parameterStatement)
                    }
                    DestructuringDeclarationTranslator.translate(
                            destructuringParameter, JsAstUtils.pureFqn(parameterName, null), innerBlockContext)
                }
            block.statements += currentVarInit
            block.statements += if (realBody is JsBlock) realBody.statements else listOfNotNull(realBody)

            return block
        }
    }

    fun translateForOverLiteralRange(literal: RangeLiteral): JsStatement {
        val startBlock = JsBlock()
        val leftExpression = Translation.translateAsExpression(literal.first, context, startBlock)
        val endBlock = JsBlock()
        val rightExpression = Translation.translateAsExpression(literal.second, context, endBlock)
        val stepBlock = JsBlock()
        val stepExpression = literal.step?.let { Translation.translateAsExpression(it, context, stepBlock) }

        context.addStatementsToCurrentBlockFrom(startBlock)
        val rangeStart = context.cacheExpressionIfNeeded(leftExpression)
        context.addStatementsToCurrentBlockFrom(endBlock)
        val rangeEnd = context.defineTemporary(rightExpression)
        context.addStatementsToCurrentBlockFrom(stepBlock)
        val step = stepExpression?.let { context.defineTemporary(it) }

        val body = translateBody(null)
        val conditionExpression = when (literal.type) {
            RangeType.RANGE_TO -> lessThanEq(parameterName.makeRef(), rangeEnd)
            RangeType.UNTIL -> lessThan(parameterName.makeRef(), rangeEnd)
            RangeType.DOWN_TO -> greaterThanEq(parameterName.makeRef(), rangeEnd)
        }.source(expression)

        val incrementExpression = if (step == null) {
            val incrementOperator = when (literal.type) {
                RangeType.RANGE_TO,
                RangeType.UNTIL -> JsUnaryOperator.INC
                RangeType.DOWN_TO -> JsUnaryOperator.DEC
            }
            JsPostfixOperation(incrementOperator, parameterName.makeRef()).source(expression)
        }
        else {
            val incrementOperator = when (literal.type) {
                RangeType.RANGE_TO,
                RangeType.UNTIL -> JsBinaryOperator.ASG_ADD
                RangeType.DOWN_TO -> JsBinaryOperator.ASG_SUB
            }
            JsBinaryOperation(incrementOperator, parameterName.makeRef(), step).source(expression)
        }

        val initVars = newVar(parameterName, rangeStart).apply { source = expression }

        return JsFor(initVars, conditionExpression, incrementExpression, body)
    }

    fun translateForOverRange(): JsStatement {
        val rangeExpression = context.defineTemporary(Translation.translateAsExpression(loopRange, context))

        fun getProperty(funName: String): JsExpression = JsNameRef(funName, rangeExpression).source(loopRange)

        val start = context.defineTemporary(getProperty("first"))
        val end = context.defineTemporary(getProperty("last"))
        val increment = context.defineTemporary(getProperty("step"))

        val body = translateBody(null)

        val conditionExpression = lessThanEq(parameterName.makeRef(), end).source(expression)
        val incrementExpression = addAssign(parameterName.makeRef(), increment).source(expression)
        val initVars = newVar(parameterName, start).apply { source = expression }

        return JsFor(initVars, conditionExpression, incrementExpression, body)
    }

    fun translateForOverArray(): JsStatement {
        val rangeExpression = context.defineTemporary(Translation.translateAsExpression(loopRange, context))
        val length = ArrayFIF.LENGTH_PROPERTY_INTRINSIC.apply(rangeExpression, listOf(), context)
        val end = context.defineTemporary(length)
        val index = context.declareTemporary(JsIntLiteral(0), expression)

        val arrayAccess = JsArrayAccess(rangeExpression, index.reference()).source(expression)
        val body = translateBody(arrayAccess)
        val initExpression = assignment(index.reference(), JsIntLiteral(0)).source(expression)
        val conditionExpression = inequality(index.reference(), end).source(expression)
        val incrementExpression = JsPrefixOperation(JsUnaryOperator.INC, index.reference()).source(expression)

        return JsFor(initExpression, conditionExpression, incrementExpression, body)
    }

    fun translateForOverArrayWithIndex(info: WithIndexInfo): JsStatement {
        val range = context.cacheExpressionIfNeeded(info.range)
        val indexVar = info.index?.let { context.getNameForDescriptor(it) } ?: JsScope.declareTemporary()
        val valueVar = info.value?.let { context.getNameForDescriptor(it) }

        val initExpression = newVar(indexVar, JsIntLiteral(0)).apply { source = expression }
        val conditionExpression = inequality(indexVar.makeRef(), JsNameRef("length", range)).source(expression)
        val incrementExpression = JsPrefixOperation(JsUnaryOperator.INC, indexVar.makeRef()).source(expression)

        val body = JsBlock()
        if (valueVar != null) {
            body.statements += newVar(valueVar, JsArrayAccess(range, indexVar.makeRef())).apply { source = expression }
        }
        expression.body?.let { body.statements += Translation.translateAsStatement(it, context.innerBlock(body)) }

        return JsFor(initExpression, conditionExpression, incrementExpression, body)
    }

    fun findCollection() =
            context.currentModule.findClassAcrossModuleDependencies(ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.collection))!!

    fun translateForOverCollectionIndices(info: WithIndexInfo): JsStatement {
        val range = context.cacheExpressionIfNeeded(info.range)
        val indexVar = info.index?.let { context.getNameForDescriptor(it) } ?: JsScope.declareTemporary()

        val initExpression = newVar(indexVar, JsIntLiteral(0)).apply { source = expression }

        val sizeDescriptor = getPropertyByName(findCollection().unsubstitutedMemberScope, Name.identifier("size"))
        val sizeName = context.getNameForDescriptor(sizeDescriptor)
        val conditionExpression = inequality(indexVar.makeRef(), JsNameRef(sizeName, range)).source(expression)

        val incrementExpression = JsPrefixOperation(JsUnaryOperator.INC, indexVar.makeRef()).source(expression)

        val body = JsBlock()
        expression.body?.let { body.statements += Translation.translateAsStatement(it, context.innerBlock(body)) }

        return JsFor(initExpression, conditionExpression, incrementExpression, body)
    }

    fun findIterable() =
        context.currentModule.findClassAcrossModuleDependencies(ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.iterable))!!

    fun findSequence() =
            context.currentModule.findClassAcrossModuleDependencies(ClassId.topLevel(sequenceFqName))!!

    fun translateForOverCollectionWithIndex(info: WithIndexInfo): JsStatement {
        val range = context.cacheExpressionIfNeeded(info.range)
        val indexVar = info.index?.let { context.getNameForDescriptor(it) }
        val valueVar = info.value?.let { context.getNameForDescriptor(it) }

        indexVar?.let { context.addStatementToCurrentBlock(newVar(it, JsIntLiteral(0)).apply { source = expression }) }

        val iteratorVar = JsScope.declareTemporary()
        val rangeOwner = if (info.receiverType == WithIndexReceiverType.SEQUENCE) findSequence() else findIterable()
        val iteratorDescriptor =  getFunctionByName(rangeOwner.unsubstitutedMemberScope, Name.identifier("iterator"))
        val iteratorName = context.getNameForDescriptor(iteratorDescriptor)
        val initExpression = newVar(iteratorVar, JsInvocation(pureFqn(iteratorName, range))).apply { source = expression }

        val iteratorClassDescriptor = iteratorDescriptor.returnType!!.constructor.declarationDescriptor as ClassDescriptor

        val hasNextDescriptor = getFunctionByName(iteratorClassDescriptor.unsubstitutedMemberScope, Name.identifier("hasNext"))
        val hasNextName = context.getNameForDescriptor(hasNextDescriptor)
        val hasNextInvocation = JsInvocation(pureFqn(hasNextName, iteratorVar.makeRef())).source(expression)

        val nextDescriptor = getFunctionByName(iteratorClassDescriptor.unsubstitutedMemberScope, Name.identifier("next"))
        val nextName = context.getNameForDescriptor(nextDescriptor)
        val nextInvocation = JsInvocation(pureFqn(nextName, iteratorVar.makeRef())).source(expression)

        val incrementExpression = indexVar?.let { JsPrefixOperation(JsUnaryOperator.INC, it.makeRef()).source(expression) }

        val body = JsBlock()
        body.statements += if (valueVar != null) {
            newVar(valueVar, nextInvocation).apply { source = expression }
        }
        else {
            asSyntheticStatement(nextInvocation)
        }
        expression.body?.let { body.statements += Translation.translateAsStatement(it, context.innerBlock(body)) }
        return JsFor(initExpression, hasNextInvocation, incrementExpression).also { it.body = body }
    }

    fun translateForOverIterator(): JsStatement {

        fun translateMethodInvocation(
                receiver: JsExpression?,
                resolvedCall: ResolvedCall<FunctionDescriptor>,
                block: JsBlock
        ): JsExpression = CallTranslator.translate(context.innerBlock(block), resolvedCall, receiver)

        fun iteratorMethodInvocation(): JsExpression {
            val range = Translation.translateAsExpression(loopRange, context)
            val resolvedCall = getIteratorFunction(context.bindingContext(), loopRange)
            return CallTranslator.translate(context, resolvedCall, range)
        }

        val iteratorVar = context.defineTemporary(iteratorMethodInvocation())

        fun hasNextMethodInvocation(block: JsBlock): JsExpression {
            val resolvedCall = getHasNextCallable(context.bindingContext(), loopRange)
            return translateMethodInvocation(iteratorVar, resolvedCall, block)
        }

        val hasNextBlock = JsBlock()
        val hasNextInvocation = hasNextMethodInvocation(hasNextBlock)

        val nextBlock = JsBlock()
        val nextInvoke = translateMethodInvocation(iteratorVar, getNextFunction(context.bindingContext(), loopRange), nextBlock)

        val bodyStatements = mutableListOf<JsStatement>()
        val exitCondition = if (hasNextBlock.isEmpty) {
            hasNextInvocation
        }
        else {
            bodyStatements += hasNextBlock.statements
            bodyStatements += JsIf(notOptimized(hasNextInvocation), JsBreak().apply { source = expression }).apply { source = expression }
            JsBooleanLiteral(true)
        }
        bodyStatements += nextBlock.statements
        bodyStatements += translateBody(nextInvoke)?.let(::flattenStatement).orEmpty()
        return JsWhile(exitCondition, bodyStatements.singleOrNull() ?: JsBlock(bodyStatements))
    }

    val rangeLiteral = extractForOverRangeLiteral()
    val withIndexCall = extractWithIndexCall()

    val result = when {
        rangeLiteral != null ->
            translateForOverLiteralRange(rangeLiteral)

        withIndexCall != null ->
            when (withIndexCall.receiverType) {
                WithIndexReceiverType.ARRAY -> translateForOverArrayWithIndex(withIndexCall)
                WithIndexReceiverType.ITERABLE,
                WithIndexReceiverType.SEQUENCE,
                WithIndexReceiverType.COLLECTION -> {
                    if (withIndexCall.value == null && withIndexCall.receiverType == WithIndexReceiverType.COLLECTION) {
                        translateForOverCollectionIndices(withIndexCall)
                    }
                    else {
                        translateForOverCollectionWithIndex(withIndexCall)
                    }
                }
            }

        isForOverRange() ->
            translateForOverRange()

        isForOverArray() ->
            translateForOverArray()

        else ->
            translateForOverIterator()
    }

    return result.apply { source = expression }
}

private enum class RangeType {
    RANGE_TO,
    UNTIL,
    DOWN_TO
}

private class RangeLiteral(val type: RangeType, val first: KtExpression, val second: KtExpression, var step: KtExpression?)

private class WithIndexInfo(
        val receiverType: WithIndexReceiverType,
        val index: VariableDescriptor?, val value: VariableDescriptor?,
        val range: JsExpression
)

private enum class WithIndexReceiverType {
    ARRAY,
    COLLECTION,
    ITERABLE,
    SEQUENCE
}