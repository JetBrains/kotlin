package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle
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
        fun isApplicable(callExpression: KtCallExpression): Boolean {
            val expression = callExpression.calleeExpression?.deparenthesize() as? KtExpression ?: return false
            val function = when (expression) {
                is KtLambdaExpression -> expression.functionLiteral
                is KtFunction -> expression
                else -> return false
            }

            val arguments = callExpression.valueArguments
            if (arguments.isNotEmpty()) return false

            val statements = function.bodyExpression?.blockExpressionsOrSingle()?.toList() ?: return false
            if (statements.isNotEmpty()) return false

            return true
        }

        fun applyTo(callExpression: KtCallExpression) {
            callExpression.delete()
        }
    }
}
