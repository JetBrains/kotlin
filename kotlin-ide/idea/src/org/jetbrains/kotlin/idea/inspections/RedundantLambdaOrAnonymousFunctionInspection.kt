package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi2ir.deparenthesize

class RedundantLambdaOrAnonymousFunctionInspection : AbstractApplicabilityBasedInspection<KtCallExpression>(KtCallExpression::class.java) {
    override fun inspectionText(element: KtCallExpression): String =
        if (element.calleeExpression?.deparenthesize() is KtFunction)
            KotlinBundle.message("inspection.redundant.anonymous.function.description")
        else
            KotlinBundle.message("inspection.redundant.lambda.description")

    override val defaultFixText: String get() = KotlinBundle.message("inspection.redundant.lambda.or.anonymous.function.fix")

    override fun isApplicable(element: KtCallExpression): Boolean = Companion.isApplicable(element)

    override fun applyTo(element: KtCallExpression, project: Project, editor: Editor?) {
        Companion.applyTo(element)
    }

    override fun inspectionHighlightType(element: KtCallExpression): ProblemHighlightType = ProblemHighlightType.LIKE_UNUSED_SYMBOL

    companion object {
        fun isApplicable(callExpression: KtCallExpression): Boolean = false
        fun applyTo(callExpression: KtCallExpression) = Unit
    }
}
