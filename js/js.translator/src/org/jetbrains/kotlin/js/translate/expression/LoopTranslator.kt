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

@file:JvmName("LoopTranslator")

package org.jetbrains.kotlin.js.translate.expression

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.CompositeFIF
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.*
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getLoopRange
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtWhileExpressionBase
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.singletonOrEmptyList

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
        val breakIfConditionIsFalseStatement = JsIf(not(jsCondition), JsBreak())
        val bodyBlock = convertToBlock(bodyStatement)
        jsCondition = JsLiteral.TRUE

        if (doWhile) {
            // translate to: tmpSecondRun = false;
            // do { if(tmpSecondRun) { <expr> if(!tmpExprVar) break; } else tmpSecondRun=true; <body> } while(true)
            val secondRun = context.defineTemporary(JsLiteral.FALSE)
            conditionBlock.statements.add(breakIfConditionIsFalseStatement)
            val ifStatement = JsIf(secondRun, conditionBlock, assignment(secondRun, JsLiteral.TRUE).makeStmt())
            bodyBlock.statements.add(0, ifStatement)
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
    return result.source(expression)!!
}

fun translateForExpression(expression: KtForExpression, context: TranslationContext): JsStatement {
    val loopRange = getLoopRange(expression)
    val rangeType = getTypeForExpression(context.bindingContext(), loopRange)

    fun isForOverRange(): Boolean {
        //TODO: long range?
        val fqn = rangeType.constructor.declarationDescriptor?.fqNameSafe ?: return false
        return fqn.asString() == "kotlin.ranges.IntRange"
    }

    fun isForOverRangeLiteral(): Boolean =
            loopRange is KtBinaryExpression && loopRange.operationToken == KtTokens.RANGE && isForOverRange()

    fun isForOverArray(): Boolean {
        return KotlinBuiltIns.isArray(rangeType) || KotlinBuiltIns.isPrimitiveArray(rangeType)
    }


    val loopParameter = expression.loopParameter!!
    val destructuringParameter: KtDestructuringDeclaration? = loopParameter.destructuringDeclaration
    val parameterName = if (destructuringParameter == null) {
        context.getNameForElement(loopParameter)
    }
    else {
        context.scope().declareTemporary()
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
                    newVar(parameterName, itemValue)
                }
                else {
                    DestructuringDeclarationTranslator.translate(
                            destructuringParameter, parameterName, itemValue, context.innerBlock(block))
                }
            block.statements += currentVarInit
            block.statements += if (realBody is JsBlock) realBody.statements else realBody.singletonOrEmptyList()

            return block
        }
    }

    // TODO: implement reverse semantics
    fun translateForOverLiteralRange(): JsStatement {
        if (loopRange !is KtBinaryExpression) throw IllegalStateException("expected JetBinaryExpression, but ${loopRange.text}")

        val startBlock = JsBlock()
        val leftExpression = TranslationUtils.translateLeftExpression(context, loopRange, startBlock)
        val endBlock = JsBlock()
        val rightExpression = TranslationUtils.translateRightExpression(context, loopRange, endBlock)
        val rangeStart = context.cacheExpressionIfNeeded(leftExpression)
        context.addStatementsToCurrentBlockFrom(startBlock)
        context.addStatementsToCurrentBlockFrom(endBlock)
        val rangeEnd = context.defineTemporary(rightExpression)

        val body = translateBody(null)
        val conditionExpression = lessThanEq(parameterName.makeRef(), rangeEnd)
        val incrementExpression = JsPostfixOperation(JsUnaryOperator.INC, parameterName.makeRef())

        return JsFor(newVar(parameterName, rangeStart), conditionExpression, incrementExpression, body)
    }

    fun translateForOverRange(): JsStatement {
        val rangeExpression = context.defineTemporary(Translation.translateAsExpression(loopRange, context))

        fun getProperty(funName: String): JsExpression = JsNameRef(funName, rangeExpression)

        val start = context.defineTemporary(getProperty("first"))
        val end = context.defineTemporary(getProperty("last"))
        val increment = context.defineTemporary(getProperty("step"))

        val body = translateBody(null)

        val conditionExpression = lessThanEq(parameterName.makeRef(), end)
        val incrementExpression = addAssign(parameterName.makeRef(), increment)

        return JsFor(newVar(parameterName, start), conditionExpression, incrementExpression, body)
    }

    fun translateForOverArray(): JsStatement {
        val rangeExpression = context.defineTemporary(Translation.translateAsExpression(loopRange, context))
        val length = CompositeFIF.LENGTH_PROPERTY_INTRINSIC.apply(rangeExpression, listOf<JsExpression>(), context)
        val end = context.defineTemporary(length)
        val index = context.declareTemporary(context.program().getNumberLiteral(0))

        val arrayAccess = JsArrayAccess(rangeExpression, index.reference())
        val body = translateBody(arrayAccess)
        val initExpression = assignment(index.reference(), context.program().getNumberLiteral(0))
        val conditionExpression = inequality(index.reference(), end)
        val incrementExpression = JsPrefixOperation(JsUnaryOperator.INC, index.reference())

        return JsFor(initExpression, conditionExpression, incrementExpression, body)
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
            bodyStatements += JsIf(notOptimized(hasNextInvocation), JsBreak())
            JsLiteral.TRUE
        }
        bodyStatements += nextBlock.statements
        bodyStatements += translateBody(nextInvoke)?.let(::flattenStatement).orEmpty()
        return JsWhile(exitCondition, bodyStatements.singleOrNull() ?: JsBlock(bodyStatements))
    }

    return when {
        isForOverRangeLiteral() ->
            translateForOverLiteralRange()

        isForOverRange() ->
            translateForOverRange()

        isForOverArray() ->
            translateForOverArray()

        else ->
            translateForOverIterator()
    }
}
