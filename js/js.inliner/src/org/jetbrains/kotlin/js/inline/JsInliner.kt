/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.inline.context.FunctionContext
import org.jetbrains.kotlin.js.inline.context.InliningContext

import org.jetbrains.kotlin.js.translate.general.AstGenerationResult

class JsInliner(
    val reporter: JsConfig.Reporter,
    val config: JsConfig,
    val trace: DiagnosticSink,
    val translationResult: AstGenerationResult
) {

    val functionContext = FunctionContext(this)

    val cycleReporter = InlinerCycleReporter(trace, functionContext)

    fun process() {
        for (fragment in translationResult.newFragments) {
            process(fragment)
        }
    }

    fun process(fragment: JsProgramFragment) {
        val fragmentScope = functionContext.scopeForFragment(fragment) ?: return

        fragmentScope.process(fragmentScope.allCode)

        fragmentScope.update()
    }

    fun process(inlineFn: InlineFunctionDefinition, call: JsInvocation?, containingScope: InliningScope) {
        cycleReporter.processInlineFunction(inlineFn.fn, call) {
            val (function, wrapperBody) = inlineFn.fn

            if (wrapperBody != null) {
                val scope = PublicInlineFunctionInliningScope(function, wrapperBody, containingScope.fragment)
                scope.process(wrapperBody)
                scope.update()
            } else {
                containingScope.process(function)
            }
        }
    }

    private fun InliningScope.process(node: JsNode) {
        InlinerImpl(this@JsInliner, this).accept(node)
    }

    fun inline(scope: InliningScope, call: JsInvocation, currentStatement: JsStatement?): InlineableResult {
        val definition = functionContext.getFunctionDefinition(call, scope)

        return cycleReporter.inlineCall(call) {

            val function = scope.importFunctionDefinition(definition)

            val inliningContext = InliningContext(currentStatement)

            val (inlineableBody, resultExpression) = FunctionInlineMutator.getInlineableCallReplacement(call, function, inliningContext)

            // body of inline function can contain call to lambdas that need to be inlined
            scope.process(inlineableBody)

            // TODO shouldn't we process the resultExpression qualifier along with the lambda inlining?
            resultExpression?.synthetic = true

            InlineableResult(JsBlock(inliningContext.previousStatements + inlineableBody), resultExpression)
        }
    }
}