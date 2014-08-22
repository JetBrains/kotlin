/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.expression.loopTranslator

import org.jetbrains.jet.lang.psi.JetWhileExpressionBase
import org.jetbrains.k2js.translate.context.TranslationContext
import com.google.dart.compiler.backend.js.ast.JsNode
import com.google.dart.compiler.backend.js.ast.JsBlock
import org.jetbrains.k2js.translate.general.Translation
import com.google.dart.compiler.backend.js.ast.JsIf
import org.jetbrains.k2js.translate.utils.JsAstUtils
import com.google.dart.compiler.backend.js.ast.JsLiteral
import com.google.dart.compiler.backend.js.ast.JsBreak
import com.google.dart.compiler.backend.js.ast.JsDoWhile
import com.google.dart.compiler.backend.js.ast.JsWhile
import com.google.dart.compiler.backend.js.ast.JsStatement
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetForExpression
import com.google.dart.compiler.backend.js.ast.JsExpression
import org.jetbrains.k2js.translate.context.TemporaryVariable
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.k2js.translate.utils.PsiUtils.getLoopBody
import org.jetbrains.k2js.translate.utils.PsiUtils.getLoopParameter
import org.jetbrains.k2js.translate.utils.PsiUtils.getLoopRange
import org.jetbrains.k2js.translate.utils.TranslationUtils
import com.google.dart.compiler.backend.js.ast.JsFor
import com.google.dart.compiler.backend.js.ast.JsVars
import org.jetbrains.jet.lexer.JetTokens
import com.google.dart.compiler.backend.js.ast.JsUnaryOperator
import com.google.dart.compiler.backend.js.ast.JsPostfixOperation
import org.jetbrains.k2js.translate.utils.TemporariesUtils.temporariesInitialization
import com.google.dart.compiler.backend.js.ast.JsNameRef
import org.jetbrains.k2js.translate.utils.BindingUtils.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassDescriptorForType
import org.jetbrains.k2js.translate.intrinsic.functions.factories.CompositeFIF
import com.google.dart.compiler.backend.js.ast.JsArrayAccess
import com.google.dart.compiler.backend.js.ast.JsPrefixOperation
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.k2js.translate.callTranslator.CallTranslator
import org.jetbrains.k2js.translate.general.AbstractTranslator
import com.google.dart.compiler.backend.js.ast.JsName
import org.jetbrains.jet.lang.psi.JetMultiDeclaration
import org.jetbrains.k2js.translate.expression.MultiDeclarationTranslator

public fun createWhile(doWhile: Boolean, expression: JetWhileExpressionBase, context: TranslationContext): JsNode {
    val conditionExpression = expression.getCondition() ?:
                              throw IllegalArgumentException("condition expression should not be null: ${expression.getText()}")
    val conditionBlock = JsBlock()
    var jsCondition = Translation.translateAsExpression(conditionExpression, context, conditionBlock)
    var bodyStatement = translateNullableExpressionAsNotNullStatement(expression.getBody(), context)
    if (!conditionBlock.isEmpty()) {
        val IfStatement = JsIf(JsAstUtils.not(jsCondition), JsBreak())
        val bodyBlock = JsAstUtils.convertToBlock(bodyStatement)
        jsCondition = JsLiteral.TRUE
        if (doWhile) {
            // translate to: tmpSecondRun = false; do { if(tmpSecondRun) { <expr> if(!tmpExprVar) break; } else tmpSecondRun=true; <body> } while(true)
            val secondRun = context.declareTemporary(JsLiteral.FALSE)
            context.addStatementToCurrentBlock(secondRun.assignmentExpression().makeStmt())
            conditionBlock.getStatements().add(IfStatement)
            bodyBlock.getStatements().add(0, JsIf(secondRun.reference(),
                                                  conditionBlock,
                                                  JsAstUtils.assignment(secondRun.reference(), JsLiteral.TRUE).makeStmt()))
        }
        else {
            // translate to: while (true) { <expr> if(!tmpExprVar) break; <body> }
            conditionBlock.getStatements().add(IfStatement)
            bodyBlock.getStatements().addAll(0, conditionBlock.getStatements())
        }
        bodyStatement = bodyBlock
    }
    val result = if (doWhile) JsDoWhile() else JsWhile()
    result.setCondition(jsCondition)
    result.setBody(bodyStatement)
    return result.source(expression)!!
}

private fun translateNullableExpressionAsNotNullStatement(nullableExpression: JetExpression?, context: TranslationContext): JsStatement =
    if (nullableExpression == null)
        context.program().getEmptyStatement()
    else
        Translation.translateAsStatement(nullableExpression, context)


// TODO: implement reverse semantics
public class RangeLiteralForTranslator private(forExpression: JetForExpression, context: TranslationContext) : ForTranslator(forExpression, context) {

    private val rangeStart: JsExpression

    private val rangeEnd: TemporaryVariable

    {
        val loopRange = getLoopRange(expression)
        assert(loopRange is JetBinaryExpression)
        val loopRangeAsBinary = (loopRange as JetBinaryExpression)
        val startBlock = JsBlock()
        var rangeStartExpression = TranslationUtils.translateLeftExpression(context, loopRangeAsBinary, startBlock)
        val endBlock = JsBlock()
        val rightExpression = TranslationUtils.translateRightExpression(context(), loopRangeAsBinary, endBlock)
        if (TranslationUtils.isCacheNeeded(rangeStartExpression)) {
            val startVar = context.declareTemporary(rangeStartExpression)
            rangeStartExpression = startVar.reference()
            context.addStatementToCurrentBlock(startVar.assignmentExpression().makeStmt())
        }
        rangeStart = rangeStartExpression
        context.addStatementsToCurrentBlockFrom(startBlock)
        context.addStatementsToCurrentBlockFrom(endBlock)
        rangeEnd = context.declareTemporary(rightExpression)
    }

    private fun translate(): JsStatement {
        context().addStatementToCurrentBlock(temporariesInitialization(rangeEnd).makeStmt())
        return JsFor(initExpression(), getCondition(), getIncrExpression(), translateBody(null))
    }

    private fun initExpression(): JsVars {
        return JsAstUtils.newVar(parameterName, rangeStart)
    }

    private fun getCondition(): JsExpression {
        return JsAstUtils.lessThanEq(parameterName.makeRef(), rangeEnd.reference())
    }

    private fun getIncrExpression(): JsExpression {
        return JsPostfixOperation(JsUnaryOperator.INC, parameterName.makeRef())
    }

    class object {

        public fun doTranslate(expression: JetForExpression, context: TranslationContext): JsStatement {
            return (RangeLiteralForTranslator(expression, context).translate())
        }

        public fun isApplicable(expression: JetForExpression, context: TranslationContext): Boolean {
            val loopRange = getLoopRange(expression)
            if (loopRange !is JetBinaryExpression) {
                return false
            }
            val isRangeToOperation = (loopRange as JetBinaryExpression).getOperationToken() == JetTokens.RANGE
            return isRangeToOperation && RangeForTranslator.isApplicable(expression, context)
        }
    }
}

public class RangeForTranslator private(forExpression: JetForExpression, context: TranslationContext) : ForTranslator(forExpression, context) {

    private val rangeExpression: TemporaryVariable
    private val start: TemporaryVariable
    private val end: TemporaryVariable
    private val increment: TemporaryVariable

    {
        rangeExpression = context.declareTemporary(Translation.translateAsExpression(getLoopRange(expression), context))
        start = context().declareTemporary(getProperty("start"))
        end = context().declareTemporary(getProperty("end"))
        increment = context().declareTemporary(getProperty("increment"))
    }

    private fun translate(): JsStatement {
        context().addStatementToCurrentBlock(temporariesInitialization(rangeExpression, start, end, increment).makeStmt())
        return generateForExpression()
    }

    private fun generateForExpression(): JsFor {
        val result = JsFor(initExpression(), getCondition(), getIncrExpression())
        result.setBody(translateBody(null))
        return result
    }

    private fun initExpression(): JsVars {
        return JsAstUtils.newVar(parameterName, start.reference())
    }

    private fun getCondition(): JsExpression {
        return JsAstUtils.lessThanEq(parameterName.makeRef(), end.reference())
    }

    private fun getIncrExpression(): JsExpression {
        return JsAstUtils.addAssign(parameterName.makeRef(), increment.reference())
    }

    private fun getProperty(funName: String): JsExpression {
        return JsNameRef(funName, rangeExpression.reference())
    }

    class object {

        public fun doTranslate(expression: JetForExpression, context: TranslationContext): JsStatement {
            return (RangeForTranslator(expression, context).translate())
        }

        public fun isApplicable(expression: JetForExpression, context: TranslationContext): Boolean {
            val loopRange = getLoopRange(expression)
            val rangeType = getTypeForExpression(context.bindingContext(), loopRange)
            //TODO: better check
            //TODO: long range?
            return getClassDescriptorForType(rangeType).getName().asString() == "IntRange"
        }
    }
}

public class ArrayForTranslator private(forExpression: JetForExpression, context: TranslationContext) : ForTranslator(forExpression, context) {

    private val loopRange: TemporaryVariable

    private val end: TemporaryVariable

    private val index: TemporaryVariable

    {
        loopRange = context.declareTemporary(Translation.translateAsExpression(getLoopRange(expression), context))

        val length = CompositeFIF.LENGTH_PROPERTY_INTRINSIC.apply(loopRange.reference(), listOf<JsExpression>(), context())
        end = context().declareTemporary(length)
        index = context().declareTemporary(program().getNumberLiteral(0))
    }

    private fun translate(): JsStatement {
        context().addStatementToCurrentBlock(temporariesInitialization(loopRange, end).makeStmt())
        return JsFor(getInitExpression(), getCondition(), getIncrementExpression(), getBody())
    }


    private fun getBody(): JsStatement {
        val arrayAccess = JsArrayAccess(loopRange.reference(), index.reference())
        return translateBody(arrayAccess)
    }

    private fun getInitExpression(): JsVars {
        return JsAstUtils.newVar(index.name(), program().getNumberLiteral(0))
    }

    private fun getCondition(): JsExpression {
        return JsAstUtils.inequality(index.reference(), end.reference())
    }

    private fun getIncrementExpression(): JsExpression {
        return JsPrefixOperation(JsUnaryOperator.INC, index.reference())
    }

    class object {

        public fun doTranslate(expression: JetForExpression, context: TranslationContext): JsStatement {
            return (ArrayForTranslator(expression, context).translate())
        }

        public fun isApplicable(expression: JetForExpression, context: TranslationContext): Boolean {
            val loopRange = getLoopRange(expression)
            val rangeType = getTypeForExpression(context.bindingContext(), loopRange)
            //TODO: better check
            //TODO: IMPORTANT!
            return getClassDescriptorForType(rangeType).getName().asString() == "Array" || getClassDescriptorForType(rangeType).getName().asString() == "IntArray"
        }
    }
}


public class IteratorForTranslator private(forExpression: JetForExpression, context: TranslationContext) : ForTranslator(forExpression, context) {
    private val iteratorVar: TemporaryVariable

    {
        iteratorVar = context.declareTemporary(iteratorMethodInvocation())
    }

    private fun translate(): JsStatement {
        context().addStatementToCurrentBlock(iteratorVar.assignmentExpression().makeStmt())
        return JsWhile(hasNextMethodInvocation(), translateBody(nextMethodInvocation()))
    }

    private fun nextMethodInvocation(): JsExpression {
        return translateMethodInvocation(iteratorVar.reference(), getNextFunction(bindingContext(), getLoopRange(expression)))
    }

    private fun hasNextMethodInvocation(): JsExpression {
        val resolvedCall = getHasNextCallable(bindingContext(), getLoopRange(expression))
        return translateMethodInvocation(iteratorVar.reference(), resolvedCall)
    }

    private fun iteratorMethodInvocation(): JsExpression {
        val rangeExpression = getLoopRange(expression)
        val range = Translation.translateAsExpression(rangeExpression, context())
        val resolvedCall = getIteratorFunction(bindingContext(), rangeExpression)
        return translateMethodInvocation(range, resolvedCall)
    }

    private fun translateMethodInvocation(receiver: JsExpression?, resolvedCall: ResolvedCall<FunctionDescriptor>): JsExpression {
        return CallTranslator.translate(context(), resolvedCall, receiver)
    }

    class object {

        public fun doTranslate(expression: JetForExpression, context: TranslationContext): JsStatement {
            return (IteratorForTranslator(expression, context).translate())
        }
    }
}

public abstract class ForTranslator protected(protected val expression: JetForExpression, context: TranslationContext) : AbstractTranslator(context) {
    protected val parameterName: JsName
    protected val multiParameter: JetMultiDeclaration?

    {
        this.multiParameter = expression.getMultiParameter()
        this.parameterName = declareParameter()
    }

    private fun declareParameter(): JsName {
        val loopParameter = getLoopParameter(expression)
        if (loopParameter != null) {
            return context().getNameForElement(loopParameter)
        }
        assert(parameterIsMultiDeclaration(), "If loopParameter is null, multi parameter must be not null")
        return context().scope().declareTemporary()
    }

    private fun parameterIsMultiDeclaration(): Boolean {
        return multiParameter != null
    }

    private fun makeCurrentVarInit(itemValue: JsExpression?): JsStatement {
        if (multiParameter == null) {
            return JsAstUtils.newVar(parameterName, itemValue)
        }
        else {
            return MultiDeclarationTranslator.translate(multiParameter!!, parameterName, itemValue, context())
        }
    }

    protected fun translateBody(itemValue: JsExpression?): JsStatement {
        val realBody = Translation.translateAsStatement(getLoopBody(expression), context())
        if (itemValue == null && !parameterIsMultiDeclaration()) {
            return realBody
        }
        else {
            val currentVarInit = makeCurrentVarInit(itemValue)
            val block = JsAstUtils.convertToBlock(realBody)
            block.getStatements().add(0, currentVarInit)
            return block
        }
    }

    class object {

        public fun translate(expression: JetForExpression, context: TranslationContext): JsStatement {
            if (RangeLiteralForTranslator.isApplicable(expression, context)) {
                return RangeLiteralForTranslator.doTranslate(expression, context)
            }
            if (RangeForTranslator.isApplicable(expression, context)) {
                return RangeForTranslator.doTranslate(expression, context)
            }
            if (ArrayForTranslator.isApplicable(expression, context)) {
                return ArrayForTranslator.doTranslate(expression, context)
            }
            return IteratorForTranslator.doTranslate(expression, context)
        }
    }
}

