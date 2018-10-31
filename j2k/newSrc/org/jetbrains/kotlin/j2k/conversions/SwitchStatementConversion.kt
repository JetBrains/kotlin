/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.PsiElement
import com.intellij.psi.controlFlow.ControlFlowFactory
import com.intellij.psi.controlFlow.ControlFlowUtil
import com.intellij.psi.controlFlow.LocalsOrMyInstanceFieldsControlFlowPolicy
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.copyTreeAndDetach
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*


class SwitchStatementConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaSwitchStatementImpl) return recurse(element)
        element.invalidate()
        element.cases.forEach { case ->
            case.statements.forEach { it.detach(case) }
            if (case is JKJavaLabelSwitchCase) {
                case.label.detach(case)
            }
        }
        val cases = switchCasesToWhenCases(element.cases)
        val whenStatement = JKKtWhenStatementImpl(element.expression, cases)
        return recurse(whenStatement)
    }

    private fun switchCasesToWhenCases(cases: List<JKJavaSwitchCase>): List<JKKtWhenCase> =
        if (cases.isEmpty()) emptyList()
        else {
            val statements = cases
                .takeWhileInclusive { it.statements.fallsThrough() }
                .flatMap { it.statements }
                .flatMap { it.singleListOrBlockStatements() }
                .takeWhile { !isSwitchBreak(it) }
                .map { it.copyTreeAndDetach() }
                .let {
                    if (it.size == 1 && cases.first().statements.singleOrNull() is JKBlockStatement)
                        listOf(JKBlockStatementImpl(JKBlockImpl(it)))
                    else it
                }
            val javaLabels = cases
                .takeWhileInclusive { it.statements.isEmpty() }

            val statementLabels = javaLabels
                .filterIsInstance<JKJavaLabelSwitchCase>()
                .map { JKKtValueWhenLabelImpl(it.label) }
            val elseLabel = javaLabels
                .find { it is JKJavaDefaultSwitchCaseImpl }
                ?.let { JKKtElseWhenLabelImpl() }
            val elseWhenCase = elseLabel?.let {
                JKKtWhenCaseImpl(listOf(it), statements.map { it.copyTreeAndDetach() }.blockOrSingle())
            }
            val mainWhenCase =
                if (statementLabels.isNotEmpty()) {
                    JKKtWhenCaseImpl(statementLabels, statements.blockOrSingle())
                } else null
            listOfNotNull(mainWhenCase) +
                    listOfNotNull(elseWhenCase) +
                    switchCasesToWhenCases(cases.drop(javaLabels.size))
        }

    private fun <T> List<T>.takeWhileInclusive(predicate: (T) -> Boolean): List<T> =
        takeWhile(predicate) + listOfNotNull(find { !predicate(it) })

    private fun List<JKStatement>.blockOrSingle(): JKStatement =
        singleOrNull()
            ?: JKBlockStatementImpl(JKBlockImpl(this))


    private fun JKStatement.singleListOrBlockStatements(): List<JKStatement> =
        when (this) {
            is JKBlockStatement -> block.statements
            else -> listOf(this)
        }

    private fun isSwitchBreak(statement: JKStatement) =
        statement is JKBreakStatement && statement !is JKBreakWithLabelStatement

    private fun List<JKStatement>.fallsThrough(): Boolean =
        all { it.fallsThrough() }

    private fun JKStatement.fallsThrough(): Boolean =
        when (this) {
            //TODO add support of this when will be added
            // is JKThrowStatement || is JKContinueStatement -> false
            is JKBreakStatement, is JKReturnStatement -> false
            is JKBlockStatement -> block.statements.fallsThrough()
            is JKIfStatement, is JKJavaSwitchStatement, is JKKtWhenStatement ->
                this.psi!!.canCompleteNormally()
            else -> true
        }

    private fun PsiElement.canCompleteNormally(): Boolean {
        val controlFlow =
            ControlFlowFactory.getInstance(project).getControlFlow(this, LocalsOrMyInstanceFieldsControlFlowPolicy.getInstance())
        val startOffset = controlFlow.getStartOffset(this)
        val endOffset = controlFlow.getEndOffset(this)
        return startOffset == -1 || endOffset == -1 || ControlFlowUtil.canCompleteNormally(controlFlow, startOffset, endOffset)
    }
}