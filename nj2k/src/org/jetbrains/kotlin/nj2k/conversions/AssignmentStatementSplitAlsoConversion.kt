/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.copyTreeAndDetach
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*

class AssignmentStatementSplitAlsoConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return recurse(
            when (element) {
                is JKBlock -> element.splitAssignmentExpressions()
                is JKStatement ->
                    element.splitAsAssignmentExpressions()?.let { JKBlockStatementImpl(JKBlockImpl(it)) }
                else -> null
            } ?: element)
    }

    private fun JKBlock.splitAssignmentExpressions(): JKBlock? {
        val newStatements = statements.map { it.splitAsAssignmentExpressions() ?: listOf(it) }
        return if (newStatements.any { it.size > 1 }) {
            JKBlockImpl(
                newStatements.flatMap { statements ->
                    if (statements.size == 1) statements.map { it.detached(this) }
                    else statements
                }
            ).withNonCodeElementsFrom(this)
        } else null
    }

    private fun JKStatement.splitAsAssignmentExpressions(): List<JKStatement>? {
        val expression = when (this) {
            is JKKtAssignmentStatement -> expression
            is JKDeclarationStatementImpl -> (declaredStatements.singleOrNull() as? JKLocalVariableImpl)?.initializer
            else -> null
        } ?: return null

        val qualifiedExpression = expression as? JKQualifiedExpression ?: return null
        val alsoCall = qualifiedExpression.selector as? JKKtAlsoCallExpression ?: return null
        val innerAssignmentStatement = alsoCall.statement as? JKKtAssignmentStatement ?: return null

        val secondAssignment = when (this) {
            is JKKtAssignmentStatement ->
                JKKtAssignmentStatementImpl(
                    ::field.detached(),
                    innerAssignmentStatement.field.copyTreeAndDetach(),
                    operator
                )
            is JKDeclarationStatement -> {
                val variable = declaredStatements.single() as JKLocalVariableImpl
                JKDeclarationStatementImpl(
                    listOf(
                        JKLocalVariableImpl(
                            variable::type.detached(),
                            variable::name.detached(),
                            innerAssignmentStatement.field.copyTreeAndDetach(),
                            variable::mutabilityElement.detached()
                        ).withNonCodeElementsFrom(variable)
                    )
                )
            }
            else -> error("expression should be either JKKtAssignmentStatement or JKDeclarationStatementImpl")
        }

        return listOf(
            JKKtAssignmentStatementImpl(
                innerAssignmentStatement::field.detached(),
                qualifiedExpression::receiver.detached(),
                innerAssignmentStatement.operator
            ),
            secondAssignment.withNonCodeElementsFrom(this)
        )
    }
}