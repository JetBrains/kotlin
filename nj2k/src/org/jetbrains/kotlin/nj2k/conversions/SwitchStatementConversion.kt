/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiElement
import com.intellij.psi.controlFlow.ControlFlowFactory
import com.intellij.psi.controlFlow.ControlFlowUtil
import com.intellij.psi.controlFlow.LocalsOrMyInstanceFieldsControlFlowPolicy
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.blockStatement
import org.jetbrains.kotlin.nj2k.runExpression
import org.jetbrains.kotlin.nj2k.tree.*



class SwitchStatementConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaSwitchStatement) return recurse(element)
        element.invalidate()
        element.cases.forEach { case ->
            case.statements.forEach { it.detach(case) }
            if (case is JKJavaLabelSwitchCase) {
                case.label.detach(case)
            }
        }
        val cases = switchCasesToWhenCases(element.cases).moveElseCaseToTheEnd()
        val whenStatement = JKKtWhenStatement(element.expression, cases)
        return recurse(whenStatement)
    }

    private fun List<JKKtWhenCase>.moveElseCaseToTheEnd(): List<JKKtWhenCase> =
        sortedBy { it.labels.any { it is JKKtElseWhenLabel } }

    private fun switchCasesToWhenCases(cases: List<JKJavaSwitchCase>): List<JKKtWhenCase> =
        if (cases.isEmpty()) emptyList()
        else {
            val statements = cases
                .takeWhileInclusive { it.statements.fallsThrough() }
                .flatMap { it.statements }
                .takeWhileInclusive { it.singleListOrBlockStatements().none { isSwitchBreak(it) } }
                .mapNotNull { statement ->
                    when {
                        statement is JKBlockStatement ->
                            blockStatement(
                                statement.block.statements
                                    .takeWhile { !isSwitchBreak(it) }
                                    .map { it.copyTreeAndDetach() }
                            ).withFormattingFrom(statement)
                        isSwitchBreak(statement) -> null
                        else -> statement.copyTreeAndDetach()
                    }
                }

            val javaLabels = cases
                .takeWhileInclusive { it.statements.isEmpty() }

            val statementLabels = javaLabels
                .filterIsInstance<JKJavaLabelSwitchCase>()
                .map { JKKtValueWhenLabel(it.label) }
            val elseLabel = javaLabels
                .find { it is JKJavaDefaultSwitchCase }
                ?.let { JKKtElseWhenLabel() }
            val elseWhenCase = elseLabel?.let { label ->
                JKKtWhenCase(listOf(label), statements.map { it.copyTreeAndDetach() }.singleBlockOrWrapToRun())
            }
            val mainWhenCase =
                if (statementLabels.isNotEmpty()) {
                    JKKtWhenCase(statementLabels, statements.singleBlockOrWrapToRun())
                } else null
            listOfNotNull(mainWhenCase) +
                    listOfNotNull(elseWhenCase) +
                    switchCasesToWhenCases(cases.drop(javaLabels.size))
        }

    private fun <T> List<T>.takeWhileInclusive(predicate: (T) -> Boolean): List<T> =
        takeWhile(predicate) + listOfNotNull(find { !predicate(it) })

    private fun List<JKStatement>.singleBlockOrWrapToRun(): JKStatement =
        singleOrNull()
            ?: JKBlockStatement(
                JKBlockImpl(map { statement ->
                    when (statement) {
                        is JKBlockStatement ->
                            JKExpressionStatement(
                                runExpression(statement, symbolProvider)
                            )
                        else -> statement
                    }
                })
            )


    private fun JKStatement.singleListOrBlockStatements(): List<JKStatement> =
        when (this) {
            is JKBlockStatement -> block.statements
            else -> listOf(this)
        }

    private fun isSwitchBreak(statement: JKStatement) =
        statement is JKBreakStatement && statement.label is JKLabelEmpty

    private fun List<JKStatement>.fallsThrough(): Boolean =
        all { it.fallsThrough() }

    private fun JKStatement.fallsThrough(): Boolean =
        when {
            this.isThrowStatement() ||
                    this is JKBreakStatement ||
                    this is JKReturnStatement ||
                    this is JKContinueStatement -> false
            this is JKBlockStatement -> block.statements.fallsThrough()
            this is JKIfElseStatement ||
                    this is JKJavaSwitchStatement ||
                    this is JKKtWhenStatement ->
                psi?.canCompleteNormally() == true
            else -> true
        }

    private fun JKStatement.isThrowStatement(): Boolean =
        (this as? JKExpressionStatement)?.expression is JKKtThrowExpression

    private fun PsiElement.canCompleteNormally(): Boolean {
        val controlFlow =
            ControlFlowFactory.getInstance(project).getControlFlow(this, LocalsOrMyInstanceFieldsControlFlowPolicy.getInstance())
        val startOffset = controlFlow.getStartOffset(this)
        val endOffset = controlFlow.getEndOffset(this)
        return startOffset == -1 || endOffset == -1 || ControlFlowUtil.canCompleteNormally(controlFlow, startOffset, endOffset)
    }
}