/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.*
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

// This conversion is for converting continue statements. For example, this adds updaters in converted while loops, and attaches
// labels named 'loop' to continue statements inner 'when' in loops and their corresponding loops for avoiding syntax errors.
// If the 'continue' keywords in 'switch' is allowed by the kotlin syntax, only converting
// JKKtConvertedFromForLoopSyntheticWhileStatement will be enough.
class ContinueStatementConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKLoopStatement) {
            convert(element)?.also { return recurse(it) }
        }

        return recurse(element)
    }

    private fun convert(loopStatement: JKLoopStatement): JKStatement? {
        // Check JKKtConvertedFromForLoopSyntheticWhileStatement and not JKWhileStatement if its parent is
        // JKKtConvertedFromForLoopSyntheticWhileStatement because setting LabeledExpression to the child of
        // JKKtConvertedFromForLoopSyntheticWhileStatement causes type error.
        if (loopStatement is JKWhileStatement && loopStatement.parent is JKKtConvertedFromForLoopSyntheticWhileStatement) {
            return null
        }

        var needLabel = false
        var labelChanged = false
        var loopLabel = "loop"

        val parentLabel = loopStatement.parent.safeAs<JKLabeledExpression>()?.labels?.firstOrNull()
        if (parentLabel != null) {
            loopLabel = parentLabel.value
            labelChanged = true
        }

        fun convertContinue(element: JKTreeElement): JKTreeElement {
            if (element !is JKContinueStatement) return applyRecursive(element, ::convertContinue)
            val elementPsi = element.psi<PsiContinueStatement>() ?: return applyRecursive(element, ::convertContinue)
            if (elementPsi.findContinuedStatement()?.toContinuedLoop() != loopStatement.psi) return applyRecursive(
                element,
                ::convertContinue
            )
            if (element.parentIsWhenCase() && element.label is JKLabelEmpty) {
                element.label = JKLabelText(JKNameIdentifier(loopLabel))
                if (!labelChanged) {
                    needLabel = true
                }
            }
            if (loopStatement !is JKKtConvertedFromForLoopSyntheticWhileStatement) {
                return applyRecursive(element, ::convertContinue)
            }
            val statements = loopStatement.forLoopUpdaters.map { it.copyTreeAndDetach() } + element.copyTreeAndDetach()
            return if (element.parent is JKBlock)
                JKBlockStatementWithoutBrackets(statements)
            else JKBlockStatement(JKBlockImpl(statements))
        }

        val body = convertContinue(loopStatement.body)
        if (!needLabel || body !is JKStatement) {
            return null
        }

        loopStatement.body = body

        if (!needLabel) {
            return loopStatement
        }

        if (loopStatement !is JKKtConvertedFromForLoopSyntheticWhileStatement) {
            return JKLabeledExpression(
                loopStatement.copyTreeAndDetach(),
                listOf(JKNameIdentifier(loopLabel))
            ).asStatement()
        }

        return JKBlockStatementWithoutBrackets(
            listOf(
                loopStatement::variableDeclaration.detached(),
                JKLabeledExpression(
                    JKWhileStatement(
                        loopStatement::condition.detached(),
                        loopStatement::body.detached()
                    ),
                    listOf(JKNameIdentifier(loopLabel))
                ).asStatement()
            )
        )
    }

    private fun JKElement.parentIsWhenCase(): Boolean {
        return when (this.parent) {
            is JKKtWhenCase -> true
            is JKLoopStatement -> false
            null -> false
            else -> this.parent?.parentIsWhenCase() ?: false
        }
    }

    private fun PsiStatement.toContinuedLoop(): PsiLoopStatement? {
        return when (this) {
            is PsiLoopStatement -> this
            is PsiLabeledStatement -> statement?.toContinuedLoop()
            else -> null
        }
    }
}
