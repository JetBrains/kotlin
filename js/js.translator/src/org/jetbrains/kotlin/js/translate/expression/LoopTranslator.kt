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

package org.jetbrains.kotlin.js.translate.expression.loopTranslator

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.expression.DestructuringDeclarationTranslator
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.CompositeFIF
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getHasNextCallable
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getIteratorFunction
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getNextFunction
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getTypeForExpression
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getLoopParameter
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getLoopRange
import org.jetbrains.kotlin.js.translate.utils.TemporariesUtils.temporariesInitialization
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtWhileExpressionBase
import org.jetbrains.kotlin.resolve.DescriptorUtils.getClassDescriptorForType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

fun createWhile(doWhile: Boolean, expression: KtWhileExpressionBase, context: TranslationContext): JsNode {
    val conditionExpression = expression.condition ?:
                              throw IllegalArgumentException("condition expression should not be null: ${expression.text}")
    val conditionBlock = JsBlock()
    var jsCondition = Translation.translateAsExpression(conditionExpression, context, conditionBlock)
    val isEmptyLoopCondition = isEmptyExpression(jsCondition)
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
            // translate to: tmpSecondRun = false; do { if(tmpSecondRun) { <expr> if(!tmpExprVar) break; } else tmpSecondRun=true; <body> } while(true)
            val secondRun = context.declareTemporary(JsLiteral.FALSE)
            context.addStatementToCurrentBlock(secondRun.assignmentExpression().makeStmt())
            if (!isEmptyLoopCondition) {
                conditionBlock.statements.add(breakIfConditionIsFalseStatement)
            }
            val ifStatement = JsIf(secondRun.reference(), conditionBlock, assignment(secondRun.reference(), JsLiteral.TRUE).makeStmt())
            bodyBlock.statements.add(0, ifStatement)
        }
        else {
            // translate to: while (true) { <expr> if(!tmpExprVar) break; <body> }
            if (isEmptyLoopCondition) {
                bodyBlock.statements.clear()
                context.addStatementsToCurrentBlockFrom(conditionBlock)
            }
            else {
                conditionBlock.statements.add(breakIfConditionIsFalseStatement)
                bodyBlock.statements.addAll(0, conditionBlock.statements)
            }
        }

        bodyStatement = bodyBlock
    }
    else if (isEmptyLoopCondition) {
        jsCondition = JsLiteral.FALSE
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
        //TODO: better check
        //TODO: long range?
        return getClassDescriptorForType(rangeType).name.asString() == "IntRange"
    }

    fun isForOverRangeLiteral(): Boolean =
            loopRange is KtBinaryExpression && loopRange.operationToken == KtTokens.RANGE && isForOverRange()

   fun isForOverArray(): Boolean {
        //TODO: better check
        //TODO: IMPORTANT!
        return getClassDescriptorForType(rangeType).name.asString() == "Array" ||
               getClassDescriptorForType(rangeType).name.asString() == "IntArray"
    }

    val destructuringParameter: KtDestructuringDeclaration? = expression.destructuringParameter;

    fun declareParameter(): JsName {
        val loopParameter = getLoopParameter(expression)
        if (loopParameter != null) {
            return context.getNameForElement(loopParameter)
        }
        assert(destructuringParameter != null) { "If loopParameter is null, multi parameter must be not null ${expression.text}" }
        return context.scope().declareTemporary()
    }

    val parameterName: JsName = declareParameter()

    fun translateBody(itemValue: JsExpression?): JsStatement? {
        val realBody = expression.body?.let { Translation.translateAsStatementAndMergeInBlockIfNeeded(it, context) }
        if (itemValue == null && destructuringParameter == null) {
            return realBody
        }
        else {
            val currentVarInit =
                if (destructuringParameter == null)
                    newVar(parameterName, itemValue)
                else
                    DestructuringDeclarationTranslator.translate(destructuringParameter, parameterName, itemValue, context)

            if (realBody == null) return JsBlock(currentVarInit)

            val block = convertToBlock(realBody)
            block.statements.add(0, currentVarInit)
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
        val rangeStart =
            if (TranslationUtils.isCacheNeeded(leftExpression)) {
                val startVar = context.declareTemporary(leftExpression)
                context.addStatementToCurrentBlock(startVar.assignmentExpression().makeStmt())
                startVar.reference()
            }
            else {
                leftExpression
            }
        context.addStatementsToCurrentBlockFrom(startBlock)
        context.addStatementsToCurrentBlockFrom(endBlock)
        val rangeEnd = context.declareTemporary(rightExpression)

        val body = translateBody(null)
        val initExpression = newVar(parameterName, rangeStart)
        val conditionExpression = lessThanEq(parameterName.makeRef(), rangeEnd.reference())
        val incrementExpression = JsPostfixOperation(JsUnaryOperator.INC, parameterName.makeRef())

        context.addStatementToCurrentBlock(temporariesInitialization(rangeEnd).makeStmt())
        return JsFor(initExpression, conditionExpression, incrementExpression, body)
    }

    fun translateForOverRange(): JsStatement {
        val rangeExpression = context.declareTemporary(Translation.translateAsExpression(loopRange, context))

        fun getProperty(funName: String): JsExpression = JsNameRef(funName, rangeExpression.reference())

        val start = context.declareTemporary(getProperty("first"))
        val end = context.declareTemporary(getProperty("last"))
        val increment = context.declareTemporary(getProperty("step"))

        val body = translateBody(null)
        val initExpression = newVar(parameterName, start.reference())
        val  conditionExpression = lessThanEq(parameterName.makeRef(), end.reference())
        val incrementExpression = addAssign(parameterName.makeRef(), increment.reference())

        context.addStatementToCurrentBlock(temporariesInitialization(rangeExpression, start, end, increment).makeStmt())
        return JsFor(initExpression, conditionExpression, incrementExpression, body)
    }

    fun translateForOverArray(): JsStatement {
        val rangeExpression  = context.declareTemporary(Translation.translateAsExpression(loopRange, context))
        val length = CompositeFIF.LENGTH_PROPERTY_INTRINSIC.apply(rangeExpression.reference(), listOf<JsExpression>(), context)
        val end = context.declareTemporary(length)
        val index = context.declareTemporary(context.program().getNumberLiteral(0))

        val arrayAccess = JsArrayAccess(rangeExpression.reference(), index.reference())
        val body = translateBody(arrayAccess)
        val initExpression = newVar(index.name(), context.program().getNumberLiteral(0))
        val conditionExpression = inequality(index.reference(), end.reference())
        val incrementExpression = JsPrefixOperation(JsUnaryOperator.INC, index.reference())

        context.addStatementToCurrentBlock(temporariesInitialization(rangeExpression, end).makeStmt())
        return JsFor(initExpression, conditionExpression, incrementExpression, body)
    }

    fun translateForOverIterator(): JsStatement {

        fun translateMethodInvocation(receiver: JsExpression?, resolvedCall: ResolvedCall<FunctionDescriptor>): JsExpression =
                CallTranslator.translate(context, resolvedCall, receiver)

        fun iteratorMethodInvocation(): JsExpression {
            val range = Translation.translateAsExpression(loopRange, context)
            val resolvedCall = getIteratorFunction(context.bindingContext(), loopRange)
            return translateMethodInvocation(range, resolvedCall)
        }

        val iteratorVar: TemporaryVariable = context.declareTemporary(iteratorMethodInvocation())

        fun nextMethodInvocation(): JsExpression =
                translateMethodInvocation(iteratorVar.reference(), getNextFunction(context.bindingContext(), loopRange))

        fun hasNextMethodInvocation(): JsExpression {
            val resolvedCall = getHasNextCallable(context.bindingContext(), loopRange)
            return translateMethodInvocation(iteratorVar.reference(), resolvedCall)
        }

        context.addStatementToCurrentBlock(iteratorVar.assignmentExpression().makeStmt())
        val nextInvoke = nextMethodInvocation()
        val body = translateBody(nextInvoke)
        return JsWhile(hasNextMethodInvocation(), body ?: nextInvoke.makeStmt())
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
