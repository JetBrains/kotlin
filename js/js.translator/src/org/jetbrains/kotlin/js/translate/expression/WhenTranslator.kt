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

package org.jetbrains.kotlin.js.translate.expression

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.operation.InOperationTranslator
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.not
import org.jetbrains.kotlin.js.translate.utils.mutator.CoercionMutator
import org.jetbrains.kotlin.js.translate.utils.mutator.LastExpressionMutator
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTextWithLocation
import org.jetbrains.kotlin.types.KotlinType

class WhenTranslator
private constructor(private val whenExpression: KtWhenExpression, context: TranslationContext) : AbstractTranslator(context) {
    private val expressionToMatch: JsExpression?
    private val type: KotlinType?

    private val isExhaustive: Boolean
        get() {
            val type = bindingContext().getType(whenExpression)
            val isStatement = type != null && KotlinBuiltIns.isUnit(type) && !type.isMarkedNullable
            return CodegenUtil.isExhaustive(bindingContext(), whenExpression, isStatement)
        }

    init {
        val subject = whenExpression.subjectExpression
        expressionToMatch = if (subject != null) context.defineTemporary(Translation.translateAsExpression(subject, context)) else null

        type = bindingContext().getType(whenExpression)
    }

    private fun translate(): JsNode {
        var currentIf: JsIf? = null
        var resultIf: JsIf? = null
        for (entry in whenExpression.entries) {
            val statementBlock = JsBlock()
            var statement = translateEntryExpression(entry, context(), statementBlock)

            if (resultIf == null && entry.isElse) {
                context().addStatementsToCurrentBlockFrom(statementBlock)
                return statement
            }
            statement = JsAstUtils.mergeStatementInBlockIfNeeded(statement, statementBlock)

            if (resultIf == null) {
                currentIf = JsAstUtils.newJsIf(translateConditions(entry, context()), statement)
                currentIf.source = entry
                resultIf = currentIf
            }
            else {
                currentIf!!
                if (entry.isElse) {
                    currentIf.elseStatement = statement
                    return resultIf
                }
                val conditionsBlock = JsBlock()
                val nextIf = JsAstUtils.newJsIf(translateConditions(entry, context().innerBlock(conditionsBlock)), statement)
                nextIf.source = entry
                val statementToAdd = JsAstUtils.mergeStatementInBlockIfNeeded(nextIf, conditionsBlock)
                currentIf.elseStatement = statementToAdd
                currentIf = nextIf
            }
        }

        if (currentIf != null && currentIf.elseStatement == null && isExhaustive) {
            val noWhenMatchedInvocation = JsInvocation(JsAstUtils.pureFqn("noWhenBranchMatched", Namer.kotlinObject()))
            currentIf.elseStatement = JsAstUtils.asSyntheticStatement(noWhenMatchedInvocation)
        }

        return if (resultIf != null) resultIf else JsNullLiteral()
    }

    private fun translateEntryExpression(
            entry: KtWhenEntry,
            context: TranslationContext,
            block: JsBlock
    ): JsStatement {
        val expressionToExecute = entry.expression ?: error("WhenEntry should have whenExpression to execute.")
        val result = Translation.translateAsStatement(expressionToExecute, context, block)
        return if (type != null) {
            LastExpressionMutator.mutateLastExpression(result, CoercionMutator(type, context))
        }
        else {
            result
        }
    }

    private fun translateConditions(entry: KtWhenEntry, context: TranslationContext): JsExpression {
        val conditions = entry.conditions
        assert(conditions.isNotEmpty()) { "When entry (not else) should have at least one condition" }

        val first = translateCondition(conditions[0], context)
        return conditions.asSequence().drop(1).fold(first) { acc, condition -> translateOrCondition(acc, condition, context) }
    }

    private fun translateOrCondition(
            leftExpression: JsExpression,
            condition: KtWhenCondition,
            context: TranslationContext
    ): JsExpression {
        val rightContext = context.innerBlock()
        val rightExpression = translateCondition(condition, rightContext)
        context.moveVarsFrom(rightContext)
        return if (rightContext.currentBlockIsEmpty()) {
            JsBinaryOperation(JsBinaryOperator.OR, leftExpression, rightExpression)
        }
        else {
            assert(rightExpression is JsNameRef) { "expected JsNameRef, but: " + rightExpression }
            val result = rightExpression as JsNameRef
            val ifStatement = JsAstUtils.newJsIf(leftExpression, JsAstUtils.assignment(result, JsBooleanLiteral(true)).makeStmt(),
                                                 rightContext.currentBlock)
            ifStatement.source = condition
            context.addStatementToCurrentBlock(ifStatement)
            result
        }
    }

    private fun translateCondition(condition: KtWhenCondition, context: TranslationContext): JsExpression {
        val patternMatchExpression = translateWhenConditionToBooleanExpression(condition, context)
        return if (isNegated(condition)) not(patternMatchExpression) else patternMatchExpression
    }

    private fun translateWhenConditionToBooleanExpression(
            condition: KtWhenCondition,
            context: TranslationContext
    ): JsExpression = when (condition) {
        is KtWhenConditionIsPattern -> translateIsCondition(condition, context)
        is KtWhenConditionWithExpression -> translateExpressionCondition(condition, context)
        is KtWhenConditionInRange -> translateRangeCondition(condition, context)
        else -> error("Unsupported when condition " + condition.javaClass)
    }

    private fun translateIsCondition(conditionIsPattern: KtWhenConditionIsPattern, context: TranslationContext): JsExpression {
        val expressionToMatch = expressionToMatch ?: error("An is-check is not allowed in when() without subject.")
        val typeReference = conditionIsPattern.typeReference ?: error("An is-check must have a type reference.")

        val result = Translation.patternTranslator(context).translateIsCheck(expressionToMatch, typeReference)
        return (result ?: JsBooleanLiteral(true)).source(conditionIsPattern)
    }

    private fun translateExpressionCondition(condition: KtWhenConditionWithExpression, context: TranslationContext): JsExpression {
        val patternExpression = condition.expression ?: error("Expression pattern should have an expression.")

        val expressionToMatch = expressionToMatch
        val patternTranslator = Translation.patternTranslator(context)
        return if (expressionToMatch == null) {
            patternTranslator.translateExpressionForExpressionPattern(patternExpression)
        }
        else {
            val type = bindingContext().getType(whenExpression.subjectExpression!!)!!
            patternTranslator.translateExpressionPattern(type, expressionToMatch, patternExpression)
        }
    }

    private fun translateRangeCondition(condition: KtWhenConditionInRange, context: TranslationContext): JsExpression {
        val expressionToMatch = expressionToMatch ?: error("Range pattern is only available for " +
                                                           "'when (C) { in ... }'  expressions: ${condition.getTextWithLocation()}")

        val subjectAliases = hashMapOf<KtExpression, JsExpression>()
        subjectAliases[whenExpression.subjectExpression!!] = expressionToMatch
        val callContext = context.innerContextWithAliasesForExpressions(subjectAliases)
        val negated = condition.operationReference.getReferencedNameElementType() === KtTokens.NOT_IN
        return InOperationTranslator(callContext, expressionToMatch, condition.rangeExpression!!, condition.operationReference,
                                     negated).translate().source(condition)
    }

    companion object {
        @JvmStatic
        fun translate(expression: KtWhenExpression, context: TranslationContext): JsNode = WhenTranslator(expression, context).translate()

        private fun isNegated(condition: KtWhenCondition): Boolean = (condition as? KtWhenConditionIsPattern)?.isNegated ?: false
    }
}
