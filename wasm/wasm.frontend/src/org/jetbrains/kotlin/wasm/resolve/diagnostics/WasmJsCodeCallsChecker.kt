/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.wasm.resolve.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.isTopLevelInPackage
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallChecker.Companion.extractStringValue
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallChecker.Companion.isJsCall
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.wasm.util.hasValidJsCodeBody

class WasmJsCallChecker(
    private val constantExpressionEvaluator: ConstantExpressionEvaluator
) : CallChecker {

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (context.isAnnotationContext || !resolvedCall.isJsCall()) return

        val containingDeclaration = context.scope.ownerDescriptor
        if (
            !(containingDeclaration is FunctionDescriptor || containingDeclaration is PropertyDescriptor) ||
            !containingDeclaration.isTopLevelInPackage()
        ) {
            context.trace.report(ErrorsWasm.JSCODE_WRONG_CONTEXT.on(reportOn))
        } else {
            when (containingDeclaration) {
                is FunctionDescriptor -> {
                    if (!containingDeclaration.hasValidJsCodeBody(context.trace.bindingContext)) {
                        context.trace.report(ErrorsWasm.JSCODE_WRONG_CONTEXT.on(reportOn))
                    } else {
                        if (containingDeclaration.isSuspend) {
                            context.trace.report(ErrorsWasm.JSCODE_UNSUPPORTED_FUNCTION_KIND.on(reportOn, "suspend function"))
                        }
                        if (containingDeclaration.isInline) {
                            context.trace.report(ErrorsWasm.JSCODE_UNSUPPORTED_FUNCTION_KIND.on(reportOn, "inline function"))
                        }
                        if (containingDeclaration.extensionReceiverParameter != null) {
                            context.trace.report(ErrorsWasm.JSCODE_UNSUPPORTED_FUNCTION_KIND.on(reportOn, "function with extension receiver"))
                        }
                        for (parameter in containingDeclaration.valueParameters) {
                            if (parameter.name.identifierOrNullIfSpecial?.isValidES5Identifier() != true) {
                                context.trace.report(ErrorsWasm.JSCODE_INVALID_PARAMETER_NAME.on(parameter.findPsi() ?: reportOn))
                            }
                        }
                    }
                }
                is PropertyDescriptor -> {
                    if (!containingDeclaration.hasValidJsCodeBody(context.trace.bindingContext)) {
                        context.trace.report(ErrorsWasm.JSCODE_WRONG_CONTEXT.on(reportOn))
                    }
                }
            }
        }

        val expression = resolvedCall.call.callElement
        if (expression !is KtCallExpression) return

        val arguments = expression.valueArgumentList?.arguments
        val argument = arguments?.firstOrNull()?.getArgumentExpression() ?: return

        val trace = TemporaryBindingTrace.create(context.trace, "WasmJsCallChecker")
        val evaluationResult = constantExpressionEvaluator.evaluateExpression(argument, trace, TypeUtils.NO_EXPECTED_TYPE)
        val code = extractStringValue(evaluationResult)

        if (code == null) {
            context.trace.report(ErrorsJs.JSCODE_ARGUMENT_SHOULD_BE_CONSTANT.on(argument))
            return
        }

        trace.commit()
    }
}