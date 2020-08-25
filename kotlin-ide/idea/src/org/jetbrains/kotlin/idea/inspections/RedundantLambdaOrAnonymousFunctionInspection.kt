package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RedundantLambdaOrAnonymousFunctionInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = callExpressionVisitor(
        fun(callExpression: KtCallExpression) {
            if (!isApplicable(callExpression)) return
            val message = if (callExpression.calleeExpression?.deparenthesize() is KtFunction)
                KotlinBundle.message("inspection.redundant.anonymous.function.description")
            else
                KotlinBundle.message("inspection.redundant.lambda.description")

            holder.registerProblem(
                callExpression,
                message,
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                RedundantLambdaOrAnonymousFunctionFix()
            )
        }
    )

    private class RedundantLambdaOrAnonymousFunctionFix : LocalQuickFix {
        override fun getFamilyName(): String = KotlinBundle.message("inspection.redundant.lambda.or.anonymous.function.fix")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val call = descriptor.psiElement as? KtCallExpression ?: return
            applyTo(call)
        }
    }

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

            return callExpression.isUsedAsStatement(callExpression.analyze(BodyResolveMode.PARTIAL_WITH_CFA))
        }

        fun applyTo(callExpression: KtCallExpression) {
            callExpression.delete()
        }
    }
}
