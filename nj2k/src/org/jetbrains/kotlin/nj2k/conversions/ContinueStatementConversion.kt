/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.*
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.tree.*

// This conversion is for converting continue statements. For example, this adds updaters in converted while loops, and attaches
// labels named 'loop' to continue statements inner 'when' in loops and their corresponding loops for avoiding syntax errors.
// If the 'continue' keywords in 'switch' is allowed by the kotlin syntax, ContinueStatementConverter, convertForInStatement and
// convertWhileStatement in this file can be removed.
class ContinueStatementConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKKtConvertedFromForLoopSyntheticWhileStatement) {
            return recurse(convertSyntheticWhileStatement(element))
        }

        if (element is JKForInStatement) {
            convertForInStatement(element)?.also { return it }
        }

        if (element is JKWhileStatement) {
            convertWhileStatement(element)?.also { return it }
        }

        return recurse(element)
    }

    private fun convertForInStatement(loopStatement: JKForInStatement): JKStatement? {
        val continueStatementConverter = ContinueStatementConverter(loopStatement, context)
        val body = continueStatementConverter.apply(loopStatement::body.detached())
        if (!continueStatementConverter.needLabel || body !is JKStatement) {
            return null
        }
        return JKLabeledExpression(
            JKForInStatement(
                loopStatement.declaration.detached(loopStatement),
                loopStatement.iterationExpression.detached(loopStatement),
                body
            ),
            listOf(loop.copyTreeAndDetach())
        ).asStatement()
    }

    private fun convertWhileStatement(loopStatement: JKWhileStatement): JKStatement? {
        if (loopStatement.parent is JKKtConvertedFromForLoopSyntheticWhileStatement) {
            return null
        }
        val continueStatementConverter = ContinueStatementConverter(loopStatement, context)
        val body = continueStatementConverter.apply(loopStatement::body.detached())
        if (!continueStatementConverter.needLabel || body !is JKStatement) {
            return null
        }
        return JKLabeledExpression(
            JKWhileStatement(
                loopStatement.condition.detached(loopStatement),
                body
            ),
            listOf(loop.copyTreeAndDetach())
        ).asStatement()
    }

    private fun convertSyntheticWhileStatement(loopStatement: JKKtConvertedFromForLoopSyntheticWhileStatement): JKStatement {
        var needLabel = false
        val continueStatementConverter = object : RecursiveApplicableConversionBase(context) {
            override fun applyToElement(element: JKTreeElement): JKTreeElement {
                if (element !is JKContinueStatement) return recurse(element)
                val elementPsi = element.psi<PsiContinueStatement>()!!
                if (elementPsi.findContinuedStatement()?.toContinuedLoop() != loopStatement.whileStatement.psi<PsiForStatement>()) {
                    return recurse(element)
                }
                if (element.parentIsWhenCase() && element.label is JKLabelEmpty) {
                    element.label = JKLabelText(loop.copyTreeAndDetach())
                    needLabel = true
                }
                val statements = loopStatement.forLoopUpdaters.map { it.copyTreeAndDetach() } + element.copyTreeAndDetach()
                return if (element.parent is JKBlock)
                    JKBlockStatementWithoutBrackets(statements)
                else JKBlockStatement(JKBlockImpl(statements))
            }
        }
        val body = continueStatementConverter.applyToElement(loopStatement.whileStatement::body.detached())
        if (needLabel) {
            return JKBlockStatementWithoutBrackets(
                listOf(
                    loopStatement::variableDeclaration.detached(),
                    JKLabeledExpression(
                        JKWhileStatement(
                            loopStatement.whileStatement::condition.detached(),
                            body as JKStatement
                        ),
                        listOf(loop.copyTreeAndDetach())
                    ).asStatement()
                )
            )
        }
        return JKKtConvertedFromForLoopSyntheticWhileStatement(
            loopStatement::variableDeclaration.detached(),
            JKWhileStatement(
                loopStatement.whileStatement::condition.detached(),
                body as JKStatement
            )
        )
    }
}

private class ContinueStatementConverter(loopStatement: JKStatement, context: NewJ2kConverterContext) {
    private val recursiveApplicableConversion = object : RecursiveApplicableConversionBase(context) {
        override fun applyToElement(element: JKTreeElement): JKTreeElement {
            if (element !is JKContinueStatement) return recurse(element)
            val elementPsi = element.psi<PsiContinueStatement>()!!
            val loopPsi = loopStatement.psi<PsiWhileStatement>()
                ?: loopStatement.psi<PsiForStatement>()
                ?: loopStatement.psi<PsiForeachStatement>()
            if (elementPsi.findContinuedStatement()?.toContinuedLoop() != loopPsi) return recurse(element)
            if (element.parentIsWhenCase() && element.label is JKLabelEmpty) {
                element.label = JKLabelText(loop.copyTreeAndDetach())
                needLabel = true
            }
            return element
        }
    }

    var needLabel = false
    fun apply(element: JKTreeElement): JKTreeElement {
        return this.recursiveApplicableConversion.applyToElement(element)
    }
}

private val loop = JKNameIdentifier("loop")

private fun JKElement.parentIsWhenCase(): Boolean {
    return when (this.parent) {
        is JKKtWhenCase -> true
        is JKWhileStatement, is JKForInStatement -> false
        null -> false
        else -> this.parent?.parent?.parentIsWhenCase() ?: false
    }
}

private fun PsiStatement.toContinuedLoop(): PsiLoopStatement? {
    return when (this) {
        is PsiLoopStatement -> this
        is PsiLabeledStatement -> statement?.toContinuedLoop()
        else -> null
    }
}
