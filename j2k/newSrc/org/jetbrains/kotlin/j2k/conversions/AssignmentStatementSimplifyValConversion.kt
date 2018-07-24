/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKStubExpressionImpl

class AssignmentStatementSimplifyValConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKKtAlsoCallExpression) return recurse(element)
        val codeBlock = (element.statement as? JKBlockStatement)?.block ?: return recurse(element)
        if (codeBlock.statements.size > 1) {
            val assignment = codeBlock.statements[1] as? JKKtAssignmentStatement ?: return recurse(element)
            val declaration = codeBlock.statements[0] as? JKDeclarationStatement ?: return recurse(element)
            when (declaration.declaredStatements.size) {
                1 -> if (assignment.expression !is JKBinaryExpression) {
                    codeBlock.statements = listOf(assignment)
                    (assignment.field as JKQualifiedExpression).receiver = declaration.extractInitializerByIndex(0)
                } else {

                }
                2 -> if (assignment.expression !is JKBinaryExpression) {
                    codeBlock.statements = listOf(assignment)
                    val arrayAccess = assignment.field as JKArrayAccessExpression
                    arrayAccess.expression = declaration.extractInitializerByIndex(0)
                    arrayAccess.indexExpression = declaration.extractInitializerByIndex(1)
                } else {

                }
            }
        }
        if (codeBlock.statements.size == 1) {
            element.statement = codeBlock.statements.first().also { codeBlock.statements = emptyList() }
        }
        return recurse(element)
    }

    private fun JKDeclarationStatement.extractInitializerByIndex(i: Int): JKExpression {
        val variable = (declaredStatements[i] as JKLocalVariable)
        return variable.initializer.also { variable.initializer = JKStubExpressionImpl() }
    }
}