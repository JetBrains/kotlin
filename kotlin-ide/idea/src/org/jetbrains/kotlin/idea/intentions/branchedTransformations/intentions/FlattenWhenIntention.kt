/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class FlattenWhenIntention : SelfTargetingIntention<KtWhenExpression>(
    KtWhenExpression::class.java,
    KotlinBundle.lazyMessage("flatten.when.expression")
) {
    override fun isApplicableTo(element: KtWhenExpression, caretOffset: Int): Boolean {
        val subject = element.subjectExpression
        if (subject != null && subject !is KtNameReferenceExpression) return false

        if (!KtPsiUtil.checkWhenExpressionHasSingleElse(element)) return false

        val elseEntry = element.entries.singleOrNull { it.isElse } ?: return false

        val innerWhen = elseEntry.expression as? KtWhenExpression ?: return false

        if (!subject.matches(innerWhen.subjectExpression)) return false
        if (!KtPsiUtil.checkWhenExpressionHasSingleElse(innerWhen)) return false

        return elseEntry.startOffset <= caretOffset && caretOffset <= innerWhen.whenKeyword.endOffset
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor?) {
        val subjectExpression = element.subjectExpression
        val nestedWhen = element.elseExpression as KtWhenExpression

        val outerEntries = element.entries
        val innerEntries = nestedWhen.entries

        val whenExpression = KtPsiFactory(element).buildExpression {
            appendFixedText("when")
            if (subjectExpression != null) {
                appendFixedText("(").appendExpression(subjectExpression).appendFixedText(")")
            }
            appendFixedText("{\n")

            for (entry in outerEntries) {
                if (entry.isElse) continue
                appendNonFormattedText(entry.text)
                appendFixedText("\n")
            }
            for (entry in innerEntries) {
                appendNonFormattedText(entry.text)
                appendFixedText("\n")
            }

            appendFixedText("}")
        } as KtWhenExpression

        val newWhen = element.replaced(whenExpression)

        val firstNewEntry = newWhen.entries[outerEntries.size - 1]
        editor?.moveCaret(firstNewEntry.textOffset)
    }
}
