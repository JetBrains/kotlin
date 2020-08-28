/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.util.parents
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInliner.CodeInliner
import org.jetbrains.kotlin.idea.intentions.LambdaToAnonymousFunctionIntention
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.OperatorNameConventions

class KotlinInlineAnonymousFunctionProcessor(
    function: KtFunction,
    private val usage: KtExpression,
    editor: Editor?,
    project: Project,
) : AbstractKotlinDeclarationInlineProcessor<KtFunction>(function, editor, project) {
    override fun findUsages(): Array<UsageInfo> = arrayOf(UsageInfo(usage))

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        Companion.performRefactoring(usage, editor)
    }

    companion object {
        fun findCallExpression(function: KtFunction): KtExpression? {
            val psiElement = function.parents
                .takeWhile { it is KtParenthesizedExpression || it is KtLambdaExpression }
                .lastOrNull()?.parent as? KtExpression

            return psiElement?.takeIf {
                it is KtCallExpression || it is KtQualifiedExpression && it.selectorExpression.isInvokeCall
            }
        }

        fun performRefactoring(usage: KtExpression, editor: Editor?) {
            val project = usage.project
            val invokeCallExpression = when (usage) {
                is KtQualifiedExpression -> usage.selectorExpression
                is KtCallExpression -> OperatorToFunctionIntention.convert(usage).second.parent
                else -> return
            } as KtCallExpression

            val qualifiedExpression = invokeCallExpression.parent as KtQualifiedExpression
            val function = findFunction(qualifiedExpression) ?: return showErrorHint(
                project,
                editor,
                KotlinBundle.message("refactoring.the.function.not.found")
            )

            val namedFunction = convertFunctionToAnonymousFunction(function) ?: return showErrorHint(
                project,
                editor,
                KotlinBundle.message("refactoring.the.function.cannot.be.converted.to.anonymous.function")
            )

            val codeToInline = createCodeToInlineForFunction(namedFunction, editor) ?: return
            val context = invokeCallExpression.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
            val resolvedCall = invokeCallExpression.getResolvedCall(context) ?: return showErrorHint(
                project,
                editor,
                KotlinBundle.message("refactoring.the.invocation.cannot.be.resolved")
            )

            CodeInliner(
                usageExpression = null,
                bindingContext = context,
                resolvedCall = resolvedCall,
                callElement = invokeCallExpression,
                inlineSetter = false,
                codeToInline = codeToInline,
            ).doInline()
        }

        private fun findFunction(qualifiedExpression: KtQualifiedExpression): KtFunction? =
            when (val expression = qualifiedExpression.receiverExpression.deparenthesize()) {
                is KtLambdaExpression -> expression.functionLiteral
                is KtNamedFunction -> expression
                else -> null
            }

        private fun convertFunctionToAnonymousFunction(function: KtFunction): KtNamedFunction? {
            return when (function) {
                is KtNamedFunction -> function
                is KtFunctionLiteral -> {
                    val lambdaExpression = function.parent as? KtLambdaExpression ?: return null
                    val descriptor = function.descriptor as? FunctionDescriptor ?: return null
                    LambdaToAnonymousFunctionIntention.convertLambdaToFunction(lambdaExpression, descriptor) as? KtNamedFunction
                }

                else -> null
            }
        }

        private fun showErrorHint(project: Project, editor: Editor?, message: @NlsContexts.DialogMessage String) {
            CommonRefactoringUtil.showErrorHint(
                project,
                editor,
                message,
                KotlinBundle.message("title.inline.function"),
                HelpID.INLINE_METHOD
            )
        }
    }
}

private val KtExpression?.isInvokeCall: Boolean
    get() {
        if (this !is KtCallExpression) return false
        val callName = calleeExpression?.text ?: return false
        return callName == OperatorNameConventions.INVOKE.asString()
    }
