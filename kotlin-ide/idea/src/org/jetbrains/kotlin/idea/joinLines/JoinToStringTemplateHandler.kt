/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.joinLines

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.util.getLineCount
import org.jetbrains.kotlin.idea.intentions.ConvertToStringTemplateIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class JoinToStringTemplateHandler : JoinRawLinesHandlerDelegate {
    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        if (file !is KtFile) return -1

        if (start == 0) return -1
        val c = document.charsSequence[start]
        val index = if (c == '\n') start - 1 else start

        val plus = file.findElementAt(index)?.takeIf { it.node?.elementType == KtTokens.PLUS } ?: return -1
        var binaryExpr = (plus.parent.parent as? KtBinaryExpression) ?: return -1
        if (!binaryExpr.joinable()) return -1

        val lineCount = binaryExpr.getLineCount()

        var parent = binaryExpr.parent
        while (parent is KtBinaryExpression && parent.joinable() && parent.getLineCount() == lineCount) {
            binaryExpr = parent
            parent = parent.parent
        }

        var rightText = ConvertToStringTemplateIntention.buildText(binaryExpr.right, false)
        var left = binaryExpr.left
        while (left is KtBinaryExpression && left.joinable()) {
            val leftLeft = (left as? KtBinaryExpression)?.left ?: break
            if (leftLeft.getLineCount() < lineCount - 1) break
            rightText = ConvertToStringTemplateIntention.buildText(left.right, false) + rightText
            left = left.left
        }

        return when (left) {
            is KtStringTemplateExpression -> {
                val offset = left.endOffset - 1
                binaryExpr.replace(createStringTemplate(left, rightText))
                offset
            }
            is KtBinaryExpression -> {
                val leftRight = left.right
                if (leftRight is KtStringTemplateExpression) {
                    val offset = leftRight.endOffset - 1
                    leftRight.replace(createStringTemplate(leftRight, rightText))
                    binaryExpr.replace(left)
                    offset
                } else {
                    -1
                }
            }
            else -> -1
        }
    }

    private fun createStringTemplate(left: KtStringTemplateExpression, rightText: String): KtStringTemplateExpression {
        val leftText = ConvertToStringTemplateIntention.buildText(left, false)
        return KtPsiFactory(left).createExpression("\"$leftText$rightText\"") as KtStringTemplateExpression
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int): Int = -1
}

private fun KtBinaryExpression.joinable(): Boolean {
    if (operationToken != KtTokens.PLUS) return false
    if (right !is KtStringTemplateExpression) return false
    val left = left
    return when (left) {
        is KtStringTemplateExpression -> true
        is KtBinaryExpression -> left.right is KtStringTemplateExpression
        else -> false
    }
}
