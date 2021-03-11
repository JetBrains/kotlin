/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contentRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import java.util.*

class ConvertToForEachFunctionCallIntention : SelfTargetingIntention<KtForExpression>(
    KtForExpression::class.java,
    KotlinBundle.lazyMessage("replace.with.a.foreach.function.call")
) {
    override fun isApplicableTo(element: KtForExpression, caretOffset: Int): Boolean {
        val rParen = element.rightParenthesis ?: return false
        if (caretOffset > rParen.endOffset) return false // available only on the loop header, not in the body
        return element.loopRange != null && element.loopParameter != null && element.body != null
    }

    override fun applyTo(element: KtForExpression, editor: Editor?) {
        val commentSaver = CommentSaver(element, saveLineBreaks = true)

        val labelName = element.getLabelName()

        val body = element.body ?: return
        val loopParameter = element.loopParameter ?: return
        val loopRange = element.loopRange ?: return

        val functionBodyArgument: Any = (body as? KtBlockExpression)?.contentRange() ?: body

        val psiFactory = KtPsiFactory(element)
        val foreachExpression = psiFactory.createExpressionByPattern(
            "$0.forEach{$1->\n$2}", loopRange, loopParameter, functionBodyArgument
        )

        val result = element.replace(foreachExpression) as KtElement
        result.findDescendantOfType<KtFunctionLiteral>()?.getContinuesWithLabel(labelName)?.forEach {
            it.replace(psiFactory.createExpression("return@forEach"))
        }

        commentSaver.restore(result)
    }

    private fun KtElement.getContinuesWithLabel(labelName: String?): List<KtContinueExpression> {
        val continueElements = ArrayList<KtContinueExpression>()

        forEachDescendantOfType<KtContinueExpression>({ it.shouldEnterForUnqualified(this) }) {
            if (it.getLabelName() == null) {
                continueElements += it
            }
        }

        if (labelName != null) {
            forEachDescendantOfType<KtContinueExpression>({ it.shouldEnterForQualified(this, labelName) }) {
                if (it.getLabelName() == labelName) {
                    continueElements += it
                }
            }
        }

        return continueElements
    }

    private fun PsiElement.shouldEnterForUnqualified(allow: PsiElement): Boolean {
        if (this == allow) return true
        if (shouldNeverEnter()) return false
        return this !is KtLoopExpression
    }

    private fun PsiElement.shouldEnterForQualified(allow: PsiElement, labelName: String): Boolean {
        if (this == allow) return true
        if (shouldNeverEnter()) return false
        return this !is KtLoopExpression || getLabelName() != labelName
    }

    private fun PsiElement.shouldNeverEnter() = this is KtLambdaExpression || this is KtClassOrObject || this is KtFunction

    private fun KtLoopExpression.getLabelName() = (parent as? KtExpressionWithLabel)?.getLabelName()
}
