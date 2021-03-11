/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.asStatement
import org.jetbrains.kotlin.nj2k.tree.*


import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ReturnStatementInLambdaExpressionConversion(context : NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    companion object {
        const val DEFAULT_LABEL_NAME = "label"
    }

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKLambdaExpression) return recurse(element)
        val statement = element.statement
        if (statement is JKReturnStatement) {
            element.statement = statement::expression.detached().asStatement()
            return recurse(element)
        }
        if (statement is JKBlockStatement) {
            val statements = statement.block.statements
            val last = statements.lastOrNull()
            if (last is JKReturnStatement) {
                statement.block.statements -= last
                statement.block.statements += last::expression.detached().asStatement()
            }
        }
        val parentMethodName = element.parent?.parent?.parent.safeAs<JKCallExpression>()?.identifier?.name
        if (parentMethodName == null) {
            val atLeastOneReturnStatementExists = applyLabelToAllReturnStatements(statement, element, DEFAULT_LABEL_NAME)
            return if (atLeastOneReturnStatementExists) {
                JKLabeledExpression(
                    recurse(element.copyTreeAndDetach()).asStatement(),
                    listOf(JKNameIdentifier(DEFAULT_LABEL_NAME))
                )
            } else recurse(element)
        }
        applyLabelToAllReturnStatements(statement, element, parentMethodName)
        return recurse(element)
    }


    private fun applyLabelToAllReturnStatements(
        statement: JKStatement,
        lambdaExpression: JKLambdaExpression,
        label: String
    ): Boolean {
        var atLeastOneReturnStatementExists = false
        fun addLabelToReturnStatement(returnStatement: JKReturnStatement) {
            if (returnStatement.label is JKLabelEmpty && returnStatement.parentOfType<JKLambdaExpression>() == lambdaExpression) {
                atLeastOneReturnStatementExists = true
                returnStatement.label = JKLabelText(JKNameIdentifier(label))
            }
        }

        fun fixAllReturnStatements(element: JKTreeElement): JKTreeElement {
            if (element !is JKReturnStatement) return applyRecursive(element, ::fixAllReturnStatements)
            addLabelToReturnStatement(element)
            return applyRecursive(element, ::fixAllReturnStatements)
        }


        applyRecursive(statement, ::fixAllReturnStatements)
        return atLeastOneReturnStatementExists
    }
}