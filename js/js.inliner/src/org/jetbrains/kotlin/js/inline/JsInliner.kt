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

    init {
        // TODO Isn't there a better way to achieve this? Also there is a bug with private inline properties
        DummyAccessorInvocationTransformer().let {
            for (fragment in translationResult.newFragments) {
                it.accept<JsGlobalBlock>(fragment.declarationBlock)
                it.accept<JsGlobalBlock>(fragment.initializerBlock)
            }
        }
    }

    val functionContext = FunctionContext(this)

    val cycleReporter = InlinerCycleReporter(trace, functionContext)

    fun process() {
        for (fragment in translationResult.newFragments) {
            process(fragment)
        }
    }

    fun process(fragment: JsProgramFragment) {
        val fragmentScope = functionContext.scopeForFragment(fragment) ?: return

        // TODO any way and/or need to visit everything inside the fragment?
        fragmentScope.process(fragment.declarationBlock)

        // TODO Atm it's placed after inliner in order not to perform the body inlining twice. Is that OK?
        // Ideally it could be moved to the coroutine transformers. The info regarding which inline function wrappers have been imported
        // on top level should be persisted for that sake. Also it going to be needed in order to avoid duplicate code.
        InlineSuspendFunctionSplitter(fragmentScope).accept(fragment.declarationBlock)

        // Mostly for the sake of post-processor
        // TODO are inline function marked with @Test possible?
        fragment.tests?.let { fragmentScope.process(it) }

        // TODO wrap in a function in order to do the post-processing
        fragmentScope.process(fragment.initializerBlock)

        fragmentScope.update()
    }

    fun process(inlineFn: InlineFunctionDefinition, call: JsInvocation?, containingScope: InliningScope) {
        if (cycleReporter.shouldProcess(inlineFn.fn, call)) {
            val (function, wrapperBody) = inlineFn.fn

            cycleReporter.withInlineFunctionDefinition(function) {
                if (wrapperBody != null) {
                    val scope = PublicInlineFunctionInliningScope(function, wrapperBody, containingScope.fragment)
                    scope.process(wrapperBody)
                    scope.update()
                } else {
                    containingScope.process(function)
                }
            }
        }
    }

    private fun InliningScope.process(node: JsNode) {
        InlinerImpl(this@JsInliner, this).accept(node)
    }

    // TODO a lot of code... Probably a bad sign
    fun inline(scope: InliningScope, call: JsInvocation, currentStatement: JsStatement?): InlineableResult {
        val definition = functionContext.getFunctionDefinition(call, scope)

        return cycleReporter.withInlining(call) {

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