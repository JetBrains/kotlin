/*
 * Copyright 2000-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.previousStatement
import org.jetbrains.kotlin.idea.util.isUnitLiteral
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.typeUtil.isUnit

class RedundantUnitExpressionInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = referenceExpressionVisitor(fun(expression) {
        if (isRedundantUnit(expression)) {
            holder.registerProblem(
                expression,
                KotlinBundle.message("redundant.unit"),
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                RemoveRedundantUnitFix()
            )
        }
    })

    companion object {
        fun isRedundantUnit(referenceExpression: KtReferenceExpression): Boolean {
            if (!referenceExpression.isUnitLiteral) return false
            val parent = referenceExpression.parent ?: return false
            if (parent is KtReturnExpression) {
                val expectedReturnType = parent.expectedReturnType() ?: return false
                return expectedReturnType.nameIfStandardType != StandardNames.FqNames.any.shortName() && !expectedReturnType.isMarkedNullable
            }

            if (parent is KtBlockExpression) {
                if (referenceExpression == parent.lastBlockStatementOrThis()) {
                    val prev = referenceExpression.previousStatement() ?: return true
                    if (prev.isUnitLiteral) return true
                    if (prev is KtDeclaration && isDynamicCall(parent)) return false
                    val prevType = prev.analyze(BodyResolveMode.PARTIAL).getType(prev)
                    if (prevType != null) {
                        return prevType.isUnit()
                    }

                    if (prev !is KtDeclaration) return false
                    if (prev !is KtFunction) return true
                    return parent.getParentOfTypesAndPredicate(
                        true,
                        KtIfExpression::class.java,
                        KtWhenExpression::class.java
                    ) { true } == null
                }

                return true
            }

            return false
        }
    }
}

private fun isDynamicCall(parent: KtBlockExpression): Boolean = parent.getStrictParentOfType<KtFunctionLiteral>()
    ?.findLambdaReturnType()
    ?.isDynamic() == true

private fun KtReturnExpression.expectedReturnType(): KotlinType? {
    val functionDescriptor = getTargetFunctionDescriptor(analyze()) ?: return null
    val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor) as? KtFunctionLiteral
    return if (functionLiteral != null)
        functionLiteral.findLambdaReturnType()
    else
        functionDescriptor.returnType
}

private fun KtFunctionLiteral.findLambdaReturnType(): KotlinType? {
    val callExpression = getStrictParentOfType<KtCallExpression>() ?: return null
    val resolvedCall = callExpression.resolveToCall() ?: return null
    val valueArgument = getStrictParentOfType<KtValueArgument>() ?: return null
    val mapping = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return null
    return mapping.valueParameter.returnType?.arguments?.lastOrNull()?.type
}

private class RemoveRedundantUnitFix : LocalQuickFix {
    override fun getName() = KotlinBundle.message("remove.redundant.unit.fix.text")

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (descriptor.psiElement as? KtReferenceExpression)?.delete()
    }
}
