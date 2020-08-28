/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.parents
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInliner.CodeInliner
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.OperatorNameConventions

class KotlinInlineAnonymousFunctionProcessor(
    function: KtNamedFunction,
    private val usage: KtExpression,
    editor: Editor?,
    project: Project,
) : AbstractKotlinDeclarationInlineProcessor<KtNamedFunction>(function, editor, project) {
    override fun findUsages(): Array<UsageInfo> = arrayOf(UsageInfo(usage))

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        Companion.performRefactoring(usage, editor)
    }

    companion object {
        fun findCallExpression(function: KtNamedFunction): KtExpression? {
            val psiElement = function.parents
                .takeWhile { it is KtParenthesizedExpression }
                .lastOrNull()?.parent as? KtExpression

            return psiElement?.takeIf {
                it is KtCallExpression || it is KtQualifiedExpression && it.selectorExpression.isInvokeCall
            }
        }

        fun performRefactoring(usage: KtExpression, editor: Editor?) {
            val invokeCallExpression = when (usage) {
                is KtQualifiedExpression -> usage.selectorExpression
                is KtCallExpression -> OperatorToFunctionIntention.convert(usage).second.parent
                else -> return
            } as KtCallExpression

            val qualifiedExpression = invokeCallExpression.parent as KtQualifiedExpression
            val function = qualifiedExpression.receiverExpression.deparenthesize() as KtNamedFunction
            val codeToInline = createCodeToInlineForFunction(function, editor) ?: return
            val context = invokeCallExpression.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
            val resolvedCall = invokeCallExpression.getResolvedCall(context) ?: return

            CodeInliner(
                usageExpression = null,
                bindingContext = context,
                resolvedCall = resolvedCall,
                callElement = invokeCallExpression,
                inlineSetter = false,
                codeToInline = codeToInline,
            ).doInline()
        }
    }
}

private val KtExpression?.isInvokeCall: Boolean
    get() {
        if (this !is KtCallExpression) return false
        val callName = calleeExpression?.text ?: return false
        return callName == OperatorNameConventions.INVOKE.asString()
    }
