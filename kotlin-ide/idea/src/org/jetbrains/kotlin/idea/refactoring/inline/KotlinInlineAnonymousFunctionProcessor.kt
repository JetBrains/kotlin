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
import org.jetbrains.kotlin.utils.addToStdlib.cast

class KotlinInlineAnonymousFunctionProcessor(
    function: KtNamedFunction,
    private val usage: KtExpression,
    editor: Editor?,
    project: Project,
) : AbstractKotlinDeclarationInlineProcessor<KtNamedFunction>(function, editor, project) {
    override fun findUsages(): Array<UsageInfo> = arrayOf(UsageInfo(usage))

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        val callExpression = when (usage) {
            is KtQualifiedExpression -> usage.selectorExpression as KtCallExpression
            is KtCallExpression -> OperatorToFunctionIntention.convert(usage).second.parent as KtCallExpression
            else -> return
        }

        val qualifiedExpression = callExpression.parent.cast<KtQualifiedExpression>()
        val function = qualifiedExpression.receiverExpression.deparenthesize() as KtNamedFunction
        val codeToInline = createCodeToInlineForFunction(function, editor) ?: return
        val context = callExpression.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
        val resolvedCall = callExpression.getResolvedCall(context) ?: return

        CodeInliner(
            usageExpression = qualifiedExpression,
            bindingContext = context,
            resolvedCall = resolvedCall,
            callElement = callExpression,
            inlineSetter = false,
            codeToInline = codeToInline,
        ).doInline()
    }

    companion object {
        fun findCallExpression(function: KtNamedFunction): KtExpression? {
            val psiElement = function.parents
                .takeWhile { it is KtParenthesizedExpression }
                .lastOrNull()?.parent as? KtExpression

            return psiElement?.takeIf { it is KtCallExpression || it is KtQualifiedExpression }
        }
    }
}
