/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunction
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

class ConvertForEachToForLoopIntention : SelfTargetingOffsetIndependentIntention<KtSimpleNameExpression>(
    KtSimpleNameExpression::class.java, KotlinBundle.lazyMessage("replace.with.a.for.loop")
) {
    companion object {
        private const val FOR_EACH_NAME = "forEach"
        private val FOR_EACH_FQ_NAMES: Set<String> by lazy {
            sequenceOf("collections", "sequences", "text", "ranges").map { "kotlin.$it.$FOR_EACH_NAME" }.toSet()
        }
    }

    override fun isApplicableTo(element: KtSimpleNameExpression): Boolean {
        if (element.getReferencedName() != FOR_EACH_NAME) return false

        val data = extractData(element) ?: return false
        if (data.functionLiteral.valueParameters.size > 1) return false
        if (data.functionLiteral.bodyExpression == null) return false

        return true
    }

    override fun applyTo(element: KtSimpleNameExpression, editor: Editor?) {
        val (expressionToReplace, receiver, functionLiteral, context) = extractData(element)!!

        val commentSaver = CommentSaver(expressionToReplace)

        val loop = generateLoop(functionLiteral, receiver, context)
        val result = expressionToReplace.replace(loop) as KtForExpression
        result.loopParameter?.also { editor?.caretModel?.moveToOffset(it.startOffset) }

        commentSaver.restore(result)
    }

    private data class Data(
        val expressionToReplace: KtExpression,
        val receiver: KtExpression,
        val functionLiteral: KtLambdaExpression,
        val context: BindingContext
    )

    private fun extractData(nameExpr: KtSimpleNameExpression): Data? {
        val parent = nameExpr.parent
        val expression = (when (parent) {
            is KtCallExpression -> parent.parent as? KtDotQualifiedExpression
            is KtBinaryExpression -> parent
            else -> null
        } ?: return null) as KtExpression //TODO: submit bug

        val context = expression.analyze()
        val resolvedCall = expression.getResolvedCall(context) ?: return null
        if (DescriptorUtils.getFqName(resolvedCall.resultingDescriptor).toString() !in FOR_EACH_FQ_NAMES) return null

        val receiver = resolvedCall.call.explicitReceiver as? ExpressionReceiver ?: return null
        val argument = resolvedCall.call.valueArguments.singleOrNull() ?: return null
        val functionLiteral = argument.getArgumentExpression() as? KtLambdaExpression ?: return null
        return Data(expression, receiver.expression, functionLiteral, context)
    }

    private fun generateLoop(functionLiteral: KtLambdaExpression, receiver: KtExpression, context: BindingContext): KtExpression {
        val factory = KtPsiFactory(functionLiteral)

        val body = functionLiteral.bodyExpression!!
        val function = functionLiteral.functionLiteral

        body.forEachDescendantOfType<KtReturnExpression> {
            if (it.getTargetFunction(context) == function) {
                it.replace(factory.createExpression("continue"))
            }
        }

        val loopRange = KtPsiUtil.safeDeparenthesize(receiver)
        val parameter = functionLiteral.valueParameters.singleOrNull()

        return factory.createExpressionByPattern("for($0 in $1){ $2 }", parameter ?: "it", loopRange, body)
    }
}
