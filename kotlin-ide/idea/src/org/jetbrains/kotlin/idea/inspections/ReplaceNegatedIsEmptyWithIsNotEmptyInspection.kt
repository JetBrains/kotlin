/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getLastParentOfTypeInRow
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class ReplaceNegatedIsEmptyWithIsNotEmptyInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return qualifiedExpressionVisitor(fun(expression) {
            if (expression.getWrappingPrefixExpressionIfAny()?.operationToken != KtTokens.EXCL) return
            val calleeExpression = expression.callExpression?.calleeExpression ?: return
            val from = calleeExpression.text
            val to = expression.invertSelectorFunction()?.callExpression?.calleeExpression?.text ?: return
            holder.registerProblem(
                calleeExpression,
                KotlinBundle.message("replace.negated.0.with.1", from, to),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                ReplaceNegatedIsEmptyWithIsNotEmptyQuickFix(from, to)
            )
        })
    }

    companion object {
        fun KtQualifiedExpression.invertSelectorFunction(): KtQualifiedExpression? {
            val callExpression = this.callExpression ?: return null
            val calleeExpression = callExpression.calleeExpression ?: return null
            val calleeText = calleeExpression.text
            val isEmptyCall = calleeText == "isEmpty"
            val isNotEmptyCall = calleeText == "isNotEmpty"
            val isBlankCall = calleeText == "isBlank"
            val isNotBlankCall = calleeText == "isNotBlank"
            if (!isEmptyCall && !isNotEmptyCall && !isBlankCall && !isNotBlankCall) return null
            if (isEmptyCall && isEmptyFunctions.none { callExpression.isCalling(FqName(it)) }
                || isNotEmptyCall && isNotEmptyFunctions.none { callExpression.isCalling(FqName(it)) }
                || isBlankCall && !callExpression.isCalling(FqName("kotlin.text.isBlank"))
                || isNotBlankCall && !callExpression.isCalling(FqName("kotlin.text.isNotBlank"))) return null
            val to = if (isEmptyCall) "isNotEmpty" else if (isNotEmptyCall) "isEmpty" else if (isBlankCall) "isNotBlank" else "isBlank"
            return KtPsiFactory(this).createExpressionByPattern(
                "$0.$to()",
                this.receiverExpression,
                reformat = false
            ) as? KtQualifiedExpression
        }
    }
}

class ReplaceNegatedIsEmptyWithIsNotEmptyQuickFix(private val from: String, private val to: String) : LocalQuickFix {
    override fun getName() = KotlinBundle.message("replace.negated.0.with.1", from, to)

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val qualifiedExpression = descriptor.psiElement.getStrictParentOfType<KtQualifiedExpression>() ?: return
        val prefixExpression = qualifiedExpression.getWrappingPrefixExpressionIfAny() ?: return
        prefixExpression.replaced(
            KtPsiFactory(qualifiedExpression).createExpressionByPattern(
                "$0.$to()",
                qualifiedExpression.receiverExpression
            )
        )
    }
}

private fun PsiElement.getWrappingPrefixExpressionIfAny() =
    (getLastParentOfTypeInRow<KtParenthesizedExpression>() ?: this).parent as? KtPrefixExpression

private val packages = listOf(
    "java.util.ArrayList",
    "java.util.HashMap",
    "java.util.HashSet",
    "java.util.LinkedHashMap",
    "java.util.LinkedHashSet",
    "kotlin.collections",
    "kotlin.collections.List",
    "kotlin.collections.Set",
    "kotlin.collections.Map",
    "kotlin.collections.MutableList",
    "kotlin.collections.MutableSet",
    "kotlin.collections.MutableMap",
    "kotlin.text"
)

private val isEmptyFunctions = packages.map { "$it.isEmpty" }

private val isNotEmptyFunctions = packages.map { "$it.isNotEmpty" }