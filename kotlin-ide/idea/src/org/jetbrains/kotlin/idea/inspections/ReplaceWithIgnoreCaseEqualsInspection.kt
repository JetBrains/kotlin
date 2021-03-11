/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceWithIgnoreCaseEqualsInspection : AbstractKotlinInspection() {
    companion object {
        private val caseConversionFunctionFqNames =
            listOf(FqName("kotlin.text.toUpperCase"), FqName("kotlin.text.toLowerCase")).associateBy { it.shortName().asString() }

        private fun KtExpression.callInfo(): Pair<KtCallExpression, String>? {
            val call = (this as? KtQualifiedExpression)?.callExpression ?: this as? KtCallExpression ?: return null
            val calleeText = call.calleeExpression?.text ?: return null
            return call to calleeText
        }

        private fun KtCallExpression.fqName(context: BindingContext): FqName? {
            return getResolvedCall(context)?.resultingDescriptor?.fqNameOrNull()
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        binaryExpressionVisitor(fun(binaryExpression: KtBinaryExpression) {
            if (binaryExpression.operationToken != KtTokens.EQEQ) return

            val (leftCall, leftCalleeText) = binaryExpression.left?.callInfo() ?: return
            val (rightCall, rightCalleeText) = binaryExpression.right?.callInfo() ?: return
            if (leftCalleeText != rightCalleeText) return
            val caseConversionFunctionFqName = caseConversionFunctionFqNames[leftCalleeText] ?: return

            val context = binaryExpression.analyze(BodyResolveMode.PARTIAL)
            val leftCallFqName = leftCall.fqName(context) ?: return
            val rightCallFqName = rightCall.fqName(context) ?: return
            if (leftCallFqName != rightCallFqName) return
            if (leftCallFqName != caseConversionFunctionFqName) return

            holder.registerProblem(
                binaryExpression,
                KotlinBundle.message("replace.with.0", "equals(..., ignoreCase = true)"),
                ReplaceFix()
            )
        })

    private class ReplaceFix : LocalQuickFix {
        override fun getName() = KotlinBundle.message("replace.with.0", "equals(..., ignoreCase = true)")

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val binary = descriptor.psiElement as? KtBinaryExpression ?: return
            val (leftCall, _) = binary.left?.callInfo() ?: return
            val (rightCall, _) = binary.right?.callInfo() ?: return
            val psiFactory = KtPsiFactory(binary)
            val leftReceiver = leftCall.getQualifiedExpressionForSelector()?.receiverExpression
            val rightReceiver = rightCall.getQualifiedExpressionForSelector()?.receiverExpression ?: psiFactory.createThisExpression()
            val newExpression = if (leftReceiver != null) {
                psiFactory.createExpressionByPattern("$0.equals($1, ignoreCase = true)", leftReceiver, rightReceiver)
            } else {
                psiFactory.createExpressionByPattern("equals($0, ignoreCase = true)", rightReceiver)
            }
            binary.replace(newExpression)
        }
    }
}
