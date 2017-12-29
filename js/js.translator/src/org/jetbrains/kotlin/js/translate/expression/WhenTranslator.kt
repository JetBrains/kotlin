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
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTextWithLocation
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.KotlinType

private typealias EntryWithConstants = Pair<List<JsExpression>, KtWhenEntry>

class WhenTranslator
private constructor(private val whenExpression: KtWhenExpression, context: TranslationContext) : AbstractTranslator(context) {
    private val expressionToMatch: JsExpression?
    private val type: KotlinType?
    private val uniqueConstants = mutableSetOf<Any>()
    private val uniqueEnumNames = mutableSetOf<String>()

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
        var resultIf: JsNode? = null
        var setWhenStatement: (JsStatement) -> Unit = { resultIf = it }

        var i = 0
        var hasElse = false
        while (i < whenExpression.entries.size) {
            val asSwitch = translateAsSwitch(i)
            if (asSwitch != null) {
                val (jsSwitch, next) = asSwitch
                setWhenStatement(jsSwitch)
                setWhenStatement = { whenStatement ->
                    jsSwitch.cases += JsDefault().apply {
                        statements += whenStatement
                        statements += JsBreak().apply { source = whenExpression }
                    }
                }
                i = next
                continue
            }

            val entry = whenExpression.entries[i++]
            val statementBlock = JsBlock()
            var statement = translateEntryExpression(entry, context(), statementBlock)

            if (resultIf == null && entry.isElse) {
                context().addStatementsToCurrentBlockFrom(statementBlock)
                return statement
            }
            statement = JsAstUtils.mergeStatementInBlockIfNeeded(statement, statementBlock)

            val conditionsBlock = JsBlock()
            if (entry.isElse) {
                hasElse = true
                setWhenStatement(statement)
                break
            }
            val jsIf = JsAstUtils.newJsIf(translateConditions(entry, context().innerBlock(conditionsBlock)), statement)
            jsIf.source = entry

            val statementToAdd = JsAstUtils.mergeStatementInBlockIfNeeded(jsIf, conditionsBlock)
            setWhenStatement(statementToAdd)
            setWhenStatement = { jsIf.elseStatement = it }
        }

        if (isExhaustive && !hasElse) {
            val noWhenMatchedInvocation = JsInvocation(JsAstUtils.pureFqn("noWhenBranchMatched", Namer.kotlinObject()))
            setWhenStatement(JsAstUtils.asSyntheticStatement(noWhenMatchedInvocation))
        }

        return if (resultIf != null) resultIf!! else JsNullLiteral()
    }

    private fun translateAsSwitch(fromIndex: Int): Pair<JsSwitch, Int>? {
        val ktSubject = whenExpression.subjectExpression ?: return null
        val subjectType = bindingContext().getType(ktSubject) ?: return null

        val dataFlow = DataFlowValueFactory.createDataFlowValue(
                ktSubject, subjectType, bindingContext(), context().declarationDescriptor ?: context().currentModule)
        val languageVersionSettings = context().config.configuration.languageVersionSettings
        val expectedTypes = bindingContext().getDataFlowInfoBefore(ktSubject).getStableTypes(dataFlow, languageVersionSettings) +
                            setOf(subjectType)
        val subject = expressionToMatch ?: return null
        var subjectSupplier = { subject }

        val enumClass = expectedTypes.asSequence().mapNotNull { it.getEnumClass() }.firstOrNull()
        val (entriesForSwitch, nextIndex) = if (enumClass != null) {
            subjectSupplier = {
                val enumBaseClass = enumClass.getSuperClassOrAny()
                val nameProperty = DescriptorUtils.getPropertyByName(enumBaseClass.unsubstitutedMemberScope, Name.identifier("name"))
                JsNameRef(context().getNameForDescriptor(nameProperty), subject)
            }
            collectEnumEntries(fromIndex, whenExpression.entries, enumClass.defaultType)
        }
        else {
            collectPrimitiveConstantEntries(fromIndex, whenExpression.entries, expectedTypes)
        }

        return if (entriesForSwitch.asSequence().map { it.first.size }.sum() > 1) {
            val switchEntries = mutableListOf<JsSwitchMember>()
            entriesForSwitch.flatMapTo(switchEntries) { (conditions, entry) ->
                val members = conditions.map {
                    JsCase().apply {
                        caseExpression = it.source(entry)
                    }
                }
                val block = JsBlock()
                val statement = translateEntryExpression(entry, context(), block)
                val lastEntry = members.last()
                lastEntry.statements += block.statements
                lastEntry.statements += statement
                lastEntry.statements += JsBreak().apply { source = entry }
                members
            }
            Pair(JsSwitch(subjectSupplier(), switchEntries).apply { source = expression }, nextIndex)
        }
        else {
            null
        }
    }

    private fun collectPrimitiveConstantEntries(
            fromIndex: Int,
            entries: List<KtWhenEntry>,
            expectedTypes: Set<KotlinType>
    ): Pair<List<EntryWithConstants>, Int> {
        return collectConstantEntries(
                fromIndex, entries,
                { constant -> expectedTypes.asSequence().mapNotNull { constant.getValue(it) }.firstOrNull() },
                { uniqueConstants.add(it) },
                {
                    when (it) {
                        is String -> JsStringLiteral(it)
                        is Int -> JsIntLiteral(it)
                        is Short -> JsIntLiteral(it.toInt())
                        is Byte -> JsIntLiteral(it.toInt())
                        is Char -> JsIntLiteral(it.toInt())
                        else -> null
                    }
                }
        )
    }

    private fun collectEnumEntries(
            fromIndex: Int,
            entries: List<KtWhenEntry>,
            expectedType: KotlinType
    ): Pair<List<EntryWithConstants>, Int> {
        return collectConstantEntries(
                fromIndex, entries,
                { (it.toConstantValue(expectedType) as? EnumValue)?.enumEntryName?.identifier },
                { uniqueEnumNames.add(it) },
                { JsStringLiteral(it) }
        )
    }

    private fun <T : Any> collectConstantEntries(
            fromIndex: Int,
            entries: List<KtWhenEntry>,
            extractor: (CompileTimeConstant<*>) -> T?,
            filter: (T) -> Boolean,
            wrapper: (T) -> JsExpression?
    ): Pair<List<EntryWithConstants>, Int> {
        val entriesForSwitch = mutableListOf<EntryWithConstants>()
        var i = fromIndex
        while (i < entries.size) {
            val entry = entries[i]
            if (entry.isElse) break

            var hasImproperConstants = false
            val constantValues = entry.conditions.mapNotNull { condition ->
                val expression = (condition as? KtWhenConditionWithExpression)?.expression
                expression?.let { ConstantExpressionEvaluator.getConstant(it, bindingContext()) }?.let(extractor) ?: run {
                    hasImproperConstants = true
                    null
                }
            }
            if (hasImproperConstants) break

            val constants = constantValues.filter(filter).mapNotNull {
                wrapper(it) ?: run {
                    hasImproperConstants = true
                    null
                }
            }
            if (hasImproperConstants) break

            if (constants.isNotEmpty()) {
                entriesForSwitch += Pair(constants, entry)
            }
            i++
        }

        return Pair(entriesForSwitch, i)
    }

    private fun KotlinType.getEnumClass(): ClassDescriptor? {
        if (isMarkedNullable) return null
        val classDescriptor = (constructor.declarationDescriptor as? ClassDescriptor)
        return if (classDescriptor?.kind == ClassKind.ENUM_CLASS && !classDescriptor.isExternal) classDescriptor else null
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
