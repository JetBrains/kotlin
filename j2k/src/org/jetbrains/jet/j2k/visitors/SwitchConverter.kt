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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.Converter
import java.util.ArrayList

class SwitchConverter(private val converter: Converter) {
    public fun convert(statement: PsiSwitchStatement): WhenStatement
            = WhenStatement(converter.convertExpression(statement.getExpression()), switchBodyToWhenEntries(statement.getBody()))

    private class Case(val label: PsiSwitchLabelStatement?, val statements: List<PsiStatement>)

    private fun switchBodyToWhenEntries(body: PsiCodeBlock?): List<WhenEntry> {
        //TODO: this code is to be changed when continue in when is supported by Kotlin

        val cases = splitToCases(body)

        val result = ArrayList<WhenEntry>()
        var pendingSelectors = ArrayList<WhenEntrySelector>()
        for ((i, case) in cases.withIndices()) {
            if (case.label == null) { // invalid switch - no case labels
                result.add(WhenEntry(listOf(ValueWhenEntrySelector(Expression.Empty).assignNoPrototype()), convertCaseStatementsToBody(cases, i)).assignNoPrototype())
                continue
            }
            pendingSelectors.add(converter.convertStatement(case.label) as WhenEntrySelector)
            if (case.statements.isNotEmpty()) {
                result.add(WhenEntry(pendingSelectors, convertCaseStatementsToBody(cases, i)).assignNoPrototype())
                pendingSelectors = ArrayList()
            }
        }
        return result
    }

    private fun splitToCases(body: PsiCodeBlock?): List<Case> {
        val cases = ArrayList<Case>()
        var currentCaseStatements = ArrayList<PsiStatement>()
        if (body != null) {
            var label: PsiSwitchLabelStatement? = null
            for (statement in body.getStatements()) {
                if (statement is PsiSwitchLabelStatement) {
                    if (label != null) {
                        cases.add(Case(label, currentCaseStatements))
                        currentCaseStatements = ArrayList()
                    }
                    label = statement
                }
                else {
                    currentCaseStatements.add(statement)
                }
            }
            if (label != null || currentCaseStatements.isNotEmpty()) {
                cases.add(Case(label, currentCaseStatements))
            }
        }

        return cases
    }

    private fun convertCaseStatements(statements: List<PsiStatement>): List<Statement> {
        val statementsToKeep = statements.filter { !isSwitchBreak(it) }
        if (statementsToKeep.size == 1) {
            val block = statementsToKeep.single() as? PsiBlockStatement
            if (block != null) {
                return listOf(converter.convertBlock(block.getCodeBlock(), true, { !isSwitchBreak(it) }))
            }
        }
        return statementsToKeep.map { converter.convertStatement(it) }
    }

    private fun convertCaseStatements(cases: List<Case>, caseIndex: Int): List<Statement> {
        val case = cases[caseIndex]
        val fallsThrough = if (caseIndex == cases.lastIndex) {
            false
        }
        else {
            val block = case.statements.singleOrNull2() as? PsiBlockStatement
            val statements = if (block != null) block.getCodeBlock().getStatements().toList() else case.statements
            !statements.any { it is PsiBreakStatement || it is PsiContinueStatement || it is PsiReturnStatement || it is PsiThrowStatement }
        }
        return if (fallsThrough) { // we fall through into the next case
            convertCaseStatements(case.statements) + convertCaseStatements(cases, caseIndex + 1)
        }
        else {
            convertCaseStatements(case.statements)
        }
    }

    private fun convertCaseStatementsToBody(cases: List<Case>, caseIndex: Int): Statement {
        val statements = convertCaseStatements(cases, caseIndex)
        return if (statements.size == 1)
            statements.single()
        else
            Block(statements, LBrace().assignNoPrototype(), RBrace().assignNoPrototype(), true).assignNoPrototype()
    }

    private fun isSwitchBreak(statement: PsiStatement) = statement is PsiBreakStatement && statement.getLabelIdentifier() == null

    private fun <T: Any> List<T>.singleOrNull2(): T? = if (size == 1) this[0] else null
}