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

package org.jetbrains.kotlin.js.resolve.diagnostics

import com.google.gwt.dev.js.parserExceptions.AbortParsingException
import com.google.gwt.dev.js.rhino.CodePosition
import com.google.gwt.dev.js.rhino.ErrorReporter
import com.google.gwt.dev.js.rhino.offsetOf
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.js.backend.ast.JsFunctionScope
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.backend.ast.JsRootScope
import org.jetbrains.kotlin.js.parser.parseExpressionOrStatement
import org.jetbrains.kotlin.js.patterns.DescriptorPredicate
import org.jetbrains.kotlin.js.patterns.PatternBuilder
import org.jetbrains.kotlin.js.resolve.LEXICAL_SCOPE_FOR_JS
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.TypeUtils

class JsCallChecker(
        private val constantExpressionEvaluator: ConstantExpressionEvaluator
) : CallChecker {

    companion object {
        private val JS_PATTERN: DescriptorPredicate = PatternBuilder.pattern("kotlin.js.js(String)")

        @JvmStatic fun <F : CallableDescriptor?> ResolvedCall<F>.isJsCall(): Boolean {
            val descriptor = resultingDescriptor
            return descriptor is SimpleFunctionDescriptor && JS_PATTERN.test(descriptor)
        }

        @JvmStatic fun extractStringValue(compileTimeConstant: CompileTimeConstant<*>?): String? {
            return ((compileTimeConstant as? TypedCompileTimeConstant<*>)?.constantValue as? StringValue)?.value
        }
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (context.isAnnotationContext || !resolvedCall.isJsCall()) return

        val expression = resolvedCall.call.callElement
        if (expression !is KtCallExpression) return

        val arguments = expression.valueArgumentList?.arguments
        val argument = arguments?.firstOrNull()?.getArgumentExpression() ?: return

        val trace = TemporaryBindingTrace.create(context.trace, "JsCallChecker")

        val evaluationResult = constantExpressionEvaluator.evaluateExpression(argument, trace, TypeUtils.NO_EXPECTED_TYPE)
        val code = extractStringValue(evaluationResult)

        if (code == null) {
            context.trace.report(ErrorsJs.JSCODE_ARGUMENT_SHOULD_BE_CONSTANT.on(argument))
            return
        }

        argument.accept(object : KtVisitor<Nothing?, Nothing?>() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Nothing?): Nothing? {
                val variableAccessing = (expression.getResolvedCall(trace.bindingContext)?.resultingDescriptor as? VariableDescriptor)
                if (variableAccessing?.isConst == false) {
                    context.trace.report(ErrorsJs.JSCODE_ARGUMENT_NON_CONST_EXPRESSION.on(expression))
                }
                return super.visitSimpleNameExpression(expression, data)
            }
        })

        trace.commit()

        val errorReporter = JsCodeErrorReporter(argument, code, context.trace)

        try {
            val parserScope = JsFunctionScope(JsRootScope(JsProgram()), "<js fun>")
            val statements = parseExpressionOrStatement(
                    code, errorReporter, parserScope, CodePosition(0, 0), reportOn.containingFile?.name ?: "<unknown file>")

            if (statements == null || statements.isEmpty()) {
                context.trace.report(ErrorsJs.JSCODE_NO_JAVASCRIPT_PRODUCED.on(argument))
            }
        } catch (e: AbortParsingException) {
            // ignore
        }

        @Suppress("UNCHECKED_CAST")
        context.trace.record(LEXICAL_SCOPE_FOR_JS, resolvedCall as ResolvedCall<FunctionDescriptor>, context.scope)
    }
}

class JsCodeErrorReporter(
        private val nodeToReport: KtExpression,
        private val code: String,
        private val trace: BindingTrace
) : ErrorReporter {
    override fun warning(message: String, startPosition: CodePosition, endPosition: CodePosition) {
        report(ErrorsJs.JSCODE_WARNING, message, startPosition, endPosition)
    }

    override fun error(message: String, startPosition: CodePosition, endPosition: CodePosition) {
        report(ErrorsJs.JSCODE_ERROR, message, startPosition, endPosition)
        throw AbortParsingException()
    }

    private fun report(
            diagnosticFactory: DiagnosticFactory1<KtExpression, JsCallData>,
            message: String,
            startPosition: CodePosition,
            endPosition: CodePosition
    ) {
        val data = when {
            nodeToReport.isConstantStringLiteral -> {
                val reportRange = TextRange(startPosition.absoluteOffset, endPosition.absoluteOffset)
                JsCallData(reportRange, message)
            }
            else -> {
                val reportRange = nodeToReport.textRange
                val codeRange = TextRange(code.offsetOf(startPosition), code.offsetOf(endPosition))
                JsCallDataWithCode(reportRange, message, code, codeRange)
            }
        }

        val parametrizedDiagnostic = diagnosticFactory.on(nodeToReport, data)
        trace.report(parametrizedDiagnostic)
    }

    private val CodePosition.absoluteOffset: Int
        get() {
            val quotesLength = nodeToReport.firstChild.textLength
            return nodeToReport.textOffset + quotesLength + code.offsetOf(this)
        }
}

private val KtExpression.isConstantStringLiteral: Boolean
    get() = this is KtStringTemplateExpression && entries.all { it is KtLiteralStringTemplateEntry }

open class JsCallData(val reportRange: TextRange, val message: String)

class JsCallDataWithCode(
        reportRange: TextRange,
        message: String,
        val code: String,
        val codeRange: TextRange
) : JsCallData(reportRange, message)
