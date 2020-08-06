/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.codegen.SamCodegenUtil
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.sam.SamConversionOracle
import org.jetbrains.kotlin.resolve.sam.SamConversionResolver
import org.jetbrains.kotlin.resolve.sam.getFunctionTypeForPossibleSamType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.keysToMapExceptNulls

class RedundantSamConstructorInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return callExpressionVisitor(fun(expression) {
            if (expression.valueArguments.isEmpty()) return

            val samConstructorCalls = samConstructorCallsToBeConverted(expression)
            if (samConstructorCalls.isEmpty()) return
            val single = samConstructorCalls.singleOrNull()
            if (single != null) {
                val calleeExpression = single.calleeExpression ?: return
                val problemDescriptor = holder.manager.createProblemDescriptor(
                    single.getQualifiedExpressionForSelector()?.receiverExpression ?: calleeExpression,
                    single.typeArgumentList ?: calleeExpression,
                    KotlinBundle.message("redundant.sam.constructor"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    isOnTheFly,
                    createQuickFix(single)
                )

                holder.registerProblem(problemDescriptor)
            } else {
                val problemDescriptor = holder.manager.createProblemDescriptor(
                    expression.valueArgumentList!!,
                    KotlinBundle.message("redundant.sam.constructors"),
                    createQuickFix(samConstructorCalls),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly
                )

                holder.registerProblem(problemDescriptor)
            }
        })
    }

    private fun createQuickFix(expression: KtCallExpression): LocalQuickFix {
        return object : LocalQuickFix {
            override fun getName() = KotlinBundle.message("remove.redundant.sam.constructor")
            override fun getFamilyName() = name
            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                if (!FileModificationService.getInstance().preparePsiElementForWrite(expression)) return
                replaceSamConstructorCall(expression)
            }
        }
    }

    private fun createQuickFix(expressions: Collection<KtCallExpression>): LocalQuickFix {
        return object : LocalQuickFix {
            override fun getName() = KotlinBundle.message("remove.redundant.sam.constructors")
            override fun getFamilyName() = name
            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                for (callExpression in expressions) {
                    if (!FileModificationService.getInstance().preparePsiElementForWrite(callExpression)) return
                    replaceSamConstructorCall(callExpression)
                }
            }
        }
    }

    companion object {
        fun replaceSamConstructorCall(callExpression: KtCallExpression): KtLambdaExpression {
            val functionalArgument = callExpression.samConstructorValueArgument()?.getArgumentExpression()
                ?: throw AssertionError("SAM-constructor should have a FunctionLiteralExpression as single argument: ${callExpression.getElementTextWithContext()}")
            return callExpression.getQualifiedExpressionForSelectorOrThis().replace(functionalArgument) as KtLambdaExpression
        }

        private fun canBeReplaced(
            parentCall: KtCallExpression,
            samConstructorCallArgumentMap: Map<KtValueArgument, KtCallExpression>
        ): Boolean {
            val context = parentCall.analyze(BodyResolveMode.PARTIAL)

            val calleeExpression = parentCall.calleeExpression ?: return false
            val scope = calleeExpression.getResolutionScope(context, calleeExpression.getResolutionFacade())

            val originalCall = parentCall.getResolvedCall(context) ?: return false

            val dataFlow = context.getDataFlowInfoBefore(parentCall)
            val callResolver = parentCall.getResolutionFacade().frontendService<CallResolver>()
            val newCall = CallWithConvertedArguments(originalCall.call, samConstructorCallArgumentMap)

            val qualifiedExpression = parentCall.getQualifiedExpressionForSelectorOrThis()
            val expectedType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, qualifiedExpression] ?: TypeUtils.NO_EXPECTED_TYPE

            val resolutionResults = callResolver.resolveFunctionCall(BindingTraceContext(), scope, newCall, expectedType, dataFlow, false)

            if (!resolutionResults.isSuccess) return false

            val generatingAdditionalSamCandidateIsDisabled =
                parentCall.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument) ||
                        parentCall.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitVarargAsArrayAfterSamArgument)

            val samAdapterOriginalDescriptor =
                if (generatingAdditionalSamCandidateIsDisabled && resolutionResults.resultingCall is NewResolvedCallImpl<*>) {
                    resolutionResults.resultingDescriptor
                } else {
                    SamCodegenUtil.getOriginalIfSamAdapter(resolutionResults.resultingDescriptor) ?: return false
                }

            return samAdapterOriginalDescriptor.original == originalCall.resultingDescriptor.original
        }

        private class CallWithConvertedArguments(
            original: Call,
            callArgumentMapToConvert: Map<KtValueArgument, KtCallExpression>,
        ) : DelegatingCall(original) {
            private val newArguments: List<ValueArgument>

            init {
                val factory = KtPsiFactory(callElement)
                newArguments = original.valueArguments.map { argument ->
                    val call = callArgumentMapToConvert[argument]
                    val newExpression = call?.samConstructorValueArgument()?.getArgumentExpression() ?: return@map argument
                    factory.createArgument(newExpression, argument.getArgumentName()?.asName, reformat = false)
                }
            }

            override fun getValueArguments() = newArguments
        }

        fun samConstructorCallsToBeConverted(functionCall: KtCallExpression): Collection<KtCallExpression> {
            val valueArguments = functionCall.valueArguments
            if (valueArguments.none { canBeSamConstructorCall(it) }) return emptyList()

            val resolutionFacade = functionCall.getResolutionFacade()
            val oracle = resolutionFacade.frontendService<SamConversionOracle>()
            val resolver = resolutionFacade.frontendService<SamConversionResolver>()

            val bindingContext = functionCall.analyze(resolutionFacade, BodyResolveMode.PARTIAL)
            val functionResolvedCall = functionCall.getResolvedCall(bindingContext) ?: return emptyList()
            if (!functionResolvedCall.isReallySuccess()) return emptyList()

            /**
             * Checks that SAM conversion for [arg] and [call] in the argument position is possible
             * and does not loose any information.
             *
             * We want to do as many cheap checks as possible before actually trying to resolve substituted call in [canBeReplaced].
             *
             * Several cases where we do not want the conversion:
             *
             * - Expected argument type is inferred from the argument; for example when the expected type is `T`, and SAM constructor
             * helps to deduce it.
             * - Expected argument type is a base type for the actual argument type; for example when expected type is `Any`, and removing
             * SAM constructor will lead to passing object of different type.
             */
            fun samConversionIsPossible(arg: KtValueArgument, call: KtCallExpression): Boolean {
                val samConstructor =
                    call.getResolvedCall(bindingContext)?.resultingDescriptor?.original as? SamConstructorDescriptor ?: return false

                // we suppose that SAM constructors return type is always not nullable
                val samConstructorReturnType = samConstructor.returnType?.unwrap()?.takeUnless { it.isNullable() } ?: return false

                // we take original parameter descriptor to get type parameter instead of inferred type (e.g. `T` instead of `Runnable`)
                val originalParameterDescriptor = functionResolvedCall.getParameterForArgument(arg)?.original ?: return false
                val expectedNotNullableType = originalParameterDescriptor.type.makeNotNullable().unwrap()

                if (resolver.getFunctionTypeForPossibleSamType(expectedNotNullableType, oracle) == null) return false

                return samConstructorReturnType.constructor == expectedNotNullableType.constructor
            }

            val argumentsWithSamConstructors = valueArguments.keysToMapExceptNulls { arg ->
                arg.toCallExpression()?.takeIf { call -> samConversionIsPossible(arg, call) }
            }

            val haveToConvertAllArguments = !functionCall.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument)

            val argumentsThatCanBeConverted = if (haveToConvertAllArguments) {
                argumentsWithSamConstructors.takeIf { it.values.none(::containsLabeledReturnPreventingConversion) }.orEmpty()
            } else {
                argumentsWithSamConstructors.filterValues { !containsLabeledReturnPreventingConversion(it) }
            }

            return when {
                argumentsThatCanBeConverted.isEmpty() -> emptyList()
                !canBeReplaced(functionCall, argumentsThatCanBeConverted) -> emptyList()
                else -> argumentsThatCanBeConverted.values
            }
        }

        private fun canBeSamConstructorCall(argument: KtValueArgument) = argument.toCallExpression()?.samConstructorValueArgument() != null

        private fun KtCallExpression.samConstructorValueArgument(): KtValueArgument? {
            return valueArguments.singleOrNull()?.takeIf { it.getArgumentExpression() is KtLambdaExpression }
        }

        private fun ValueArgument.toCallExpression(): KtCallExpression? {
            val argumentExpression = getArgumentExpression()
            return (if (argumentExpression is KtDotQualifiedExpression)
                argumentExpression.selectorExpression
            else
                argumentExpression) as? KtCallExpression
        }

        private fun containsLabeledReturnPreventingConversion(it: KtCallExpression): Boolean {
            val samValueArgument = it.samConstructorValueArgument()
            val samConstructorName = (it.calleeExpression as? KtSimpleNameExpression)?.getReferencedNameAsName()
            return samValueArgument == null ||
                    samConstructorName == null ||
                    samValueArgument.hasLabeledReturnPreventingConversion(samConstructorName)
        }

        private fun KtValueArgument.hasLabeledReturnPreventingConversion(samConstructorName: Name) =
            anyDescendantOfType<KtReturnExpression> { it.getLabelNameAsName() == samConstructorName }
    }
}
