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

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.ast.*
import java.util.*

class SwitchConverter(private val codeConverter: CodeConverter) {
    fun convert(statement: PsiSwitchStatement): WhenStatement
            = WhenStatement(codeConverter.convertExpression(statement.expression), switchBodyToWhenEntries(statement.body))

    private class Case(val label: PsiSwitchLabelStatement?, val statements: List<PsiStatement>)

    private fun switchBodyToWhenEntries(body: PsiCodeBlock?): List<WhenEntry> {
        //TODO: this code is to be changed when continue in when is supported by Kotlin

        val cases = splitToCases(body)

        val result = ArrayList<WhenEntry>()
        var pendingSelectors = ArrayList<WhenEntrySelector>()
        for ((i, case) in cases.withIndex()) {
            if (case.label == null) { // invalid switch - no case labels
                result.add(WhenEntry(listOf(ValueWhenEntrySelector(Expression.Empty).assignNoPrototype()), convertCaseStatementsToBody(cases, i)).assignNoPrototype())
                continue
            }
            pendingSelectors.add(codeConverter.convertStatement(case.label) as WhenEntrySelector)
            if (!case.label.isDefaultCase && case.statements.isEmpty() && cases[i + 1]?.label!!.isDefaultCase){
                result.add(WhenEntry(pendingSelectors, convertCaseStatementsToBody(cases, i + 1)).assignNoPrototype())
                pendingSelectors = ArrayList()
            }else if (case.statements.isNotEmpty()) {
                result.add(WhenEntry(pendingSelectors, convertCaseStatementsToBody(cases, i)).assignNoPrototype())
                pendingSelectors = ArrayList()
            }
        }
        return result
    }

    private fun splitToCases(body: PsiCodeBlock?): List<Case> {
        val cases = ArrayList<Case>()
        if (body != null) {
            var currentLabel: PsiSwitchLabelStatement? = null
            var currentCaseStatements = ArrayList<PsiStatement>()

            fun flushCurrentCase() {
                if (currentLabel != null || currentCaseStatements.isNotEmpty()) {
                    cases.add(Case(currentLabel, currentCaseStatements))
                }
            }

            for (statement in body.statements) {
                if (statement is PsiSwitchLabelStatement) {
                    flushCurrentCase()
                    currentLabel = statement
                    currentCaseStatements = ArrayList()
                }
                else {
                    currentCaseStatements.add(statement)
                }
            }

            flushCurrentCase()
        }

        return cases
    }

    private fun convertCaseStatements(statements: List<PsiStatement>, allowBlock: Boolean = true): List<Statement> {
        val statementsToKeep = statements.filter { !isSwitchBreak(it) }
        if (allowBlock && statementsToKeep.size == 1) {
            val block = statementsToKeep.single() as? PsiBlockStatement
            if (block != null) {
                return listOf(codeConverter.convertBlock(block.codeBlock, true, { !isSwitchBreak(it) }))
            }
        }
        return statementsToKeep.map { codeConverter.convertStatement(it) }
    }

    private fun convertCaseStatements(cases: List<Case>, caseIndex: Int, allowBlock: Boolean = true): List<Statement> {
        val case = cases[caseIndex]
        val fallsThrough = if (caseIndex == cases.lastIndex) {
            false
        }
        else {
            val block = case.statements.singleOrNull() as? PsiBlockStatement
            val statements = if (block != null) block.codeBlock.statements.toList() else case.statements
            !statements.any { it is PsiBreakStatement || it is PsiContinueStatement || it is PsiReturnStatement || it is PsiThrowStatement }
        }
        return if (fallsThrough) // we fall through into the next case
            convertCaseStatements(case.statements, allowBlock = false) + convertCaseStatements(cases, caseIndex + 1, allowBlock = false)
        else
            convertCaseStatements(case.statements, allowBlock)
    }

    private fun convertCaseStatementsToBody(cases: List<Case>, caseIndex: Int): Statement {
        val statements = convertCaseStatements(cases, caseIndex)
        return if (statements.size == 1)
            statements.single()
        else
            Block.of(statements).assignNoPrototype()
    }

    private fun isSwitchBreak(statement: PsiStatement) = statement is PsiBreakStatement && statement.labelIdentifier == null
}
