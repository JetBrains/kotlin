/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtWhenConditionInRange
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.isDynamic

object JsDynamicCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val callee = resolvedCall.resultingDescriptor
        if (callee.dispatchReceiverParameter?.type?.isDynamic() != true) return

        val element = resolvedCall.call.callElement
        if (element is KtArrayAccessExpression && element.indexExpressions.size > 1) {
            context.trace.report(ErrorsJs.WRONG_OPERATION_WITH_DYNAMIC.on(reportOn, "indexed access with more than one index"))
        }

        if (element is KtWhenConditionInRange) {
            reportInOperation(context, reportOn)
        }
        else if (element is KtBinaryExpression) {
            val token = element.operationToken
            when (token) {
                in OperatorConventions.IN_OPERATIONS -> {
                    reportInOperation(context, reportOn)
                }
                KtTokens.RANGE -> {
                    context.trace.report(ErrorsJs.WRONG_OPERATION_WITH_DYNAMIC.on(reportOn, "`..` operation"))
                }
            }
        }
    }

    private fun reportInOperation(context: CallCheckerContext, reportOn: PsiElement) {
        context.trace.report(ErrorsJs.WRONG_OPERATION_WITH_DYNAMIC.on(reportOn, "`in` operation"))
    }
}