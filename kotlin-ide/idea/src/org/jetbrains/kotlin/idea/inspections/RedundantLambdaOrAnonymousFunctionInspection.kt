package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.inline.KotlinInlineAnonymousFunctionProcessor
import org.jetbrains.kotlin.psi.*

class RedundantLambdaOrAnonymousFunctionInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : KtVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            processExpression(function)
        }

        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            processExpression(lambdaExpression.functionLiteral)
        }

        private fun processExpression(function: KtFunction) {
            if (findCallIfApplicableTo(function) == null) return
            val message = if (function is KtNamedFunction)
                KotlinBundle.message("inspection.redundant.anonymous.function.description")
            else
                KotlinBundle.message("inspection.redundant.lambda.description")

            holder.registerProblem(function, message, RedundantLambdaOrAnonymousFunctionFix())
        }
    }

    private class RedundantLambdaOrAnonymousFunctionFix : LocalQuickFix {
        override fun getFamilyName(): String = KotlinBundle.message("inspection.redundant.lambda.or.anonymous.function.fix")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val function = descriptor.psiElement as? KtFunction ?: return
            val call = KotlinInlineAnonymousFunctionProcessor.findCallExpression(function) ?: return
            KotlinInlineAnonymousFunctionProcessor(function, call, function.findExistingEditor(), project).run()
        }
    }

    companion object {
        fun findCallIfApplicableTo(function: KtFunction): KtExpression? = if (function.hasBody())
            KotlinInlineAnonymousFunctionProcessor.findCallExpression(function)
        else
            null
    }
}
