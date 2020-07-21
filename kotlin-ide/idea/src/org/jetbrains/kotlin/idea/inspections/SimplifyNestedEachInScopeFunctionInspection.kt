/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.KotlinType

class SimplifyNestedEachInScopeFunctionInspection : AbstractKotlinInspection() {

    private companion object {
        private val scopeFunctions = mapOf("also" to listOf(FqName("kotlin.also")), "apply" to listOf(FqName("kotlin.apply")))
        private val iterateFunctions = mapOf(
            "forEach" to listOf(FqName("kotlin.collections.forEach"), FqName("kotlin.text.forEach")),
            "onEach" to listOf(FqName("kotlin.collections.onEach"), FqName("kotlin.text.onEach"))
        )

        private fun KtCallExpression.getCallingShortNameOrNull(shortNamesToFqNames: Map<String, List<FqName>>): String? {
            val shortName = calleeExpression?.text ?: return null
            val names = shortNamesToFqNames[shortName] ?: return null
            val call = this.resolveToCall() ?: return null
            return if (names.any(call::isCalling)) shortName
            else null
        }

        private fun KtCallExpression.singleLambdaExpression(): KtLambdaExpression? =
            this.valueArguments.singleOrNull()?.getArgumentExpression()?.unpackLabelAndLambdaExpression()?.second

        private fun KtExpression.unpackLabelAndLambdaExpression(): Pair<KtLabeledExpression?, KtLambdaExpression?> = when (this) {
            is KtLambdaExpression -> null to this
            is KtLabeledExpression -> this to baseExpression?.unpackLabelAndLambdaExpression()?.second
            is KtAnnotatedExpression -> baseExpression?.unpackLabelAndLambdaExpression() ?: null to null
            else -> null to null
        }

        private fun KtCallExpression.getReceiverType(context: BindingContext): KotlinType? {
            val callee = calleeExpression as? KtNameReferenceExpression ?: return null
            val calleeDescriptor = context[REFERENCE_TARGET, callee] as? CallableMemberDescriptor ?: return null
            return (calleeDescriptor.dispatchReceiverParameter ?: calleeDescriptor.extensionReceiverParameter)?.type
        }

        // Ignores type parameters
        private fun KotlinType.isAssignableFrom(subtype: KotlinType) = constructor in subtype.allSupertypes().map(KotlinType::constructor)

        private fun KotlinType.allSupertypes(): Set<KotlinType> = constructor.supertypes.flatMapTo(HashSet()) { it.allSupertypes() } + this

        private abstract class ReferenceTreeVisitor : KtTreeVisitorVoid() {
            var referenced = false
                protected set
        }

        private class LabelReferenceVisitor(val labelName: String) : ReferenceTreeVisitor() {
            override fun visitExpressionWithLabel(expression: KtExpressionWithLabel) {
                if (expression.getLabelName() == labelName) referenced = true
            }
        }

        private class ParameterReferenceTreeVisitor(name: String?) : ReferenceTreeVisitor() {
            val name = name ?: "it"
            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                if (expression.getReferencedName() == name) referenced = true
            }

            override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                // Do not parse reference when name is shadowed in lambda
                if (lambdaExpression.valueParameters.none { it.name == name })
                    super.visitLambdaExpression(lambdaExpression)
            }
        }

        private class ImplicitThisReferenceVisitor(
            val matchType: KotlinType,
            val context: BindingContext,
            val thisTypes: List<KotlinType> = emptyList()
        ) : ReferenceTreeVisitor() {
            fun KotlinType.typeMatchesOutermostThis(): Boolean {
                if (!this.isAssignableFrom(matchType)) return false
                return thisTypes.none { this.isAssignableFrom(it) }
            }

            override fun visitThisExpression(expression: KtThisExpression) {
                if (expression.labelQualifier == null)
                    referenced = true // Be safe to prevent false positives
            }

            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                run label@{
                    val thisExpression = expression.receiverExpression as? KtThisExpression ?: return@label
                    val callExpression = expression.selectorExpression as? KtCallExpression ?: return@label
                    if (thisExpression.labelQualifier != null) return@label
                    if (callExpression.getReceiverType(context)?.typeMatchesOutermostThis() == true) return
                }

                super.visitDotQualifiedExpression(expression)
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                if (expression.getStrictParentOfType<KtDotQualifiedExpression>() == null) {
                    if (expression.getReceiverType(context)?.typeMatchesOutermostThis() == true) referenced = true
                }
                expression.singleLambdaExpression()?.let { lambdaExpression ->
                    val lambdaReceiverType = lambdaExpression.getType(context)?.getReceiverTypeFromFunctionType()
                    val visitor = ImplicitThisReferenceVisitor(
                        matchType,
                        context,
                        thisTypes.let { if (lambdaReceiverType != null) it.plus(lambdaReceiverType) else it }
                    )
                    lambdaExpression.bodyExpression?.acceptChildren(visitor)
                    if (visitor.referenced) referenced = true
                } ?: super.visitCallExpression(expression)
            }
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = callExpressionVisitor(fun(callExpression) {
        val scopeFunctionShortName = callExpression.getCallingShortNameOrNull(scopeFunctions) ?: return
        val arg = callExpression.valueArguments.singleOrNull() ?: return
        val (labelExpression, lambdaExpression) = when (arg) {
            is KtLambdaArgument -> arg.getArgumentExpression()?.unpackLabelAndLambdaExpression() ?: return
            else -> return
        }
        val innerExpression = lambdaExpression?.bodyExpression?.statements?.singleOrNull() ?: return
        val innerCallExpression = when (innerExpression) {
            is KtDotQualifiedExpression -> innerExpression.selectorExpression as? KtCallExpression ?: return
            is KtCallExpression -> innerExpression
            else -> return
        }
        val innerCallShortName = innerCallExpression.getCallingShortNameOrNull(iterateFunctions) ?: return

        val context = callExpression.analyze()

        val lambdaType = callExpression.getResolvedCall(context)?.getParameterForArgument(arg)?.type?.arguments?.first()?.type ?: return

        val forEachLambda = innerCallExpression.singleLambdaExpression()

        val visitors: List<ReferenceTreeVisitor> = listOfNotNull(
            LabelReferenceVisitor(labelExpression?.getLabelName() ?: scopeFunctionShortName),
            when (scopeFunctionShortName) {
                "also" -> {
                    if (innerExpression !is KtDotQualifiedExpression) return
                    val receiverExpression = innerExpression.receiverExpression
                    if (receiverExpression !is KtReferenceExpression) return
                    val parameterName = lambdaExpression.valueParameters.singleOrNull()?.name ?: "it"
                    if (!receiverExpression.textMatches(parameterName)) return

                    if (forEachLambda != null && forEachLambda.valueParameters.singleOrNull()?.name ?: "it" == parameterName)
                        null // Parameter from outer lambda is shadowed
                    else ParameterReferenceTreeVisitor(parameterName)
                }
                "apply" -> {
                    val receiverType = innerCallExpression.getReceiverType(context) ?: return
                    if (!receiverType.isAssignableFrom(lambdaType)) return
                    if (innerExpression is KtDotQualifiedExpression) {
                        val receiverExpression = innerExpression.receiverExpression
                        if (receiverExpression !is KtThisExpression) return
                        val labelName = receiverExpression.getLabelName()
                        if (labelName != null && labelName != labelExpression?.getLabelName() ?: scopeFunctionShortName) return
                    }

                    ImplicitThisReferenceVisitor(lambdaType, context)
                }
                else -> return
            }
        )

        innerCallExpression.singleLambdaExpression()?.bodyExpression?.let {
            visitors.forEach(it::accept)
        }

        if (visitors.any(ReferenceTreeVisitor::referenced)) return

        holder.registerProblem(
            callExpression.calleeExpression ?: return,
            KotlinBundle.message("nested.1.call.in.0.could.be.simplified.to.2", scopeFunctionShortName, innerCallShortName, "onEach"),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            NestedForEachFix(innerCallShortName)
        )
    })

    private class NestedForEachFix(val forEachCall: String) : LocalQuickFix {
        override fun getName() = KotlinBundle.message("simplify.call.fix.text", forEachCall, "onEach")
        override fun getFamilyName() = KotlinBundle.message("replace.0.with.1", "nested calls", "onEach")

        private class ReplaceLabelVisitor(val factory: KtPsiFactory) : KtTreeVisitorVoid() {
            override fun visitExpressionWithLabel(expression: KtExpressionWithLabel) {
                if (expression.getLabelName() != "forEach") return
                val expressionText = expression.text
                expression.replace(
                    factory.createExpression(
                        expressionText.replaceRange(
                            expressionText.indexOf('@') + 1,
                            expressionText.length,
                            "onEach"
                        )
                    )
                )
            }
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val callExpression = descriptor.psiElement.parent as? KtCallExpression ?: return
            val outerBlock = callExpression.valueArguments.singleOrNull() ?: return
            val (label, lambda) = outerBlock.getArgumentExpression()?.unpackLabelAndLambdaExpression() ?: return
            val eachCall = when (val statement = lambda?.bodyExpression?.statements?.singleOrNull()) {
                is KtDotQualifiedExpression -> statement.selectorExpression as? KtCallExpression ?: return
                is KtCallExpression -> statement
                else -> return
            }
            val factory = KtPsiFactory(project)
            val innerBlock = eachCall.valueArguments.singleOrNull() ?: return
            if (label?.labelQualifier == null && eachCall.calleeExpression?.text == "forEach")
                innerBlock.accept(ReplaceLabelVisitor(factory))
            outerBlock.replace(innerBlock)
            descriptor.psiElement.replace(factory.createExpression("onEach"))
        }
    }
}