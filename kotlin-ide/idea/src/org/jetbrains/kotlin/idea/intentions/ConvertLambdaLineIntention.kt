/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.inspections.RedundantSemicolonInspection
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace

sealed class ConvertLambdaLineIntention(private val toMultiLine: Boolean) : SelfTargetingIntention<KtLambdaExpression>(
    KtLambdaExpression::class.java,
    KotlinBundle.lazyMessage("intention.convert.lambda.line", 1.takeIf { toMultiLine } ?: 0),
) {
    override fun isApplicableTo(element: KtLambdaExpression, caretOffset: Int): Boolean {
        val functionLiteral = element.functionLiteral
        val body = functionLiteral.bodyBlockExpression ?: return false
        val startLine = functionLiteral.getLineNumber(start = true)
        val endLine = functionLiteral.getLineNumber(start = false)
        return if (toMultiLine) {
            startLine == endLine
        } else {
            if (startLine == endLine) return false
            val allChildren = body.allChildren
            if (allChildren.any { it is PsiComment && it.node.elementType == KtTokens.EOL_COMMENT }) return false
            val first = allChildren.first?.getNextSiblingIgnoringWhitespace(withItself = true) ?: return true
            val last = allChildren.last?.getPrevSiblingIgnoringWhitespace(withItself = true)
            first.getLineNumber(start = true) == last?.getLineNumber(start = false)
        }
    }

    override fun applyTo(element: KtLambdaExpression, editor: Editor?) {
        val functionLiteral = element.functionLiteral
        val body = functionLiteral.bodyBlockExpression ?: return
        val psiFactory = KtPsiFactory(element)
        if (toMultiLine) {
            body.allChildren.forEach {
                if (it.node.elementType == KtTokens.SEMICOLON) {
                    body.addAfter(psiFactory.createNewLine(), it)
                    if (RedundantSemicolonInspection.isRedundantSemicolon(it)) it.delete()
                }
            }
        }
        val bodyText = body.text
        val startLineBreak = if (toMultiLine) "\n" else ""
        val endLineBreak = if (toMultiLine && bodyText != "") "\n" else ""
        element.replace(
            psiFactory.createLambdaExpression(
                functionLiteral.valueParameters.joinToString { it.text },
                "$startLineBreak$bodyText$endLineBreak"
            )
        )
    }
}

class ConvertLambdaToMultiLineIntention : ConvertLambdaLineIntention(toMultiLine = true)

class ConvertLambdaToSingleLineIntention : ConvertLambdaLineIntention(toMultiLine = false)
