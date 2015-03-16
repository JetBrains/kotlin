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
import com.google.gwt.dev.js.rhino.*
import com.google.gwt.dev.js.rhino.Utils.*
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.ParametrizedDiagnostic
import org.jetbrains.kotlin.js.patterns.DescriptorPredicate
import org.jetbrains.kotlin.js.patterns.PatternBuilder
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.JetStringTemplateExpression
import org.jetbrains.kotlin.renderer.Renderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.JetType

import com.intellij.openapi.util.TextRange
import java.io.StringReader

import kotlin.platform.platformStatic
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace

public class JsCallChecker : CallChecker {

    class object {
        private val JS_PATTERN: DescriptorPredicate = PatternBuilder.pattern("kotlin.js.js(String)")

        platformStatic
        public fun <F : CallableDescriptor?> ResolvedCall<F>.isJsCall(): Boolean {
            val descriptor = getResultingDescriptor()
            return descriptor is SimpleFunctionDescriptor && JS_PATTERN.apply(descriptor)
        }
    }

    override fun <F : CallableDescriptor?> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        if (context.isAnnotationContext || !resolvedCall.isJsCall()) return

        val expression = resolvedCall.getCall().getCallElement()
        if (expression !is JetCallExpression) return

        val arguments = expression.getValueArgumentList()?.getArguments()
        val argument = arguments?.firstOrNull()?.getArgumentExpression()

        if (argument == null) return

        val stringType = KotlinBuiltIns.getInstance().getStringType()
        val trace = TemporaryBindingTrace.create(context.trace, "JsCallChecker")
        val evaluationResult = ConstantExpressionEvaluator.evaluate(argument, trace, stringType)

        if (evaluationResult == null) {
            context.trace.report(ErrorsJs.JSCODE_ARGUMENT_SHOULD_BE_CONSTANT.on(argument))
            return
        }

        val code = evaluationResult.getValue() as String
        val reader = StringReader(code)
        val errorReporter = JsCodeErrorReporter(argument, code, context.trace)
        Context.enter().setErrorReporter(errorReporter)

        try {
            val ts = TokenStream(reader, "js", 0)
            val parser = Parser(IRFactory(ts), /* insideFunction = */ true)
            parser.parse(ts)
        } catch (e: AbortParsingException) {
            // ignore
        } finally {
            Context.exit()
        }
    }
}

class JsCodeErrorReporter(
        private val nodeToReport: JetExpression,
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
            diagnosticFactory: DiagnosticFactory1<JetExpression, JsCallData>,
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
                val reportRange = nodeToReport.getTextRange()
                val codeRange = TextRange(code.offsetOf(startPosition), code.offsetOf(endPosition))
                JsCallDataWithCode(reportRange, message, code, codeRange)
            }
        }

        val parametrizedDiagnostic = diagnosticFactory.on(nodeToReport, data)
        trace.report(parametrizedDiagnostic)
    }

    private val CodePosition.absoluteOffset: Int
        get() {
            val quotesLength = nodeToReport.getFirstChild().getTextLength()
            return nodeToReport.getTextOffset() + quotesLength + code.offsetOf(this)
        }
}

/**
 * Calculates an offset from the start of a text for a position,
 * defined by line and offset in that line.
 */
private fun String.offsetOf(position: CodePosition): Int {
    var i = 0
    var lineCount = 0
    var offsetInLine = 0

    while (i < length()) {
        val c = charAt(i)

        if (lineCount == position.line && offsetInLine == position.offset) {
            return i
        }

        i++
        offsetInLine++

        if (isEndOfLine(c.toInt())) {
            offsetInLine = 0
            lineCount++
            assert(lineCount <= position.line)
        }
    }

    return length()
}

private val JetExpression.isConstantStringLiteral: Boolean
    get() = this is JetStringTemplateExpression && getEntries().all { it is JetLiteralStringTemplateEntry }

open class JsCallData(val reportRange: TextRange, val message: String)

class JsCallDataWithCode(
        reportRange: TextRange,
        message: String,
        val code: String,
        val codeRange: TextRange
) : JsCallData(reportRange, message)
