/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.inline.context.FunctionDefinitionLoader
import org.jetbrains.kotlin.js.inline.context.InliningContext

import org.jetbrains.kotlin.js.translate.general.AstGenerationResult

class JsInliner(
    val reporter: JsConfig.Reporter,
    val config: JsConfig,
    val trace: DiagnosticSink,
    val translationResult: AstGenerationResult
) {

    val functionDefinitionLoader = FunctionDefinitionLoader(this)

    val cycleReporter = InlinerCycleReporter(trace, functionDefinitionLoader)

    fun process() {
        for (fragment in translationResult.newFragments) {
            ImportIntoFragmentInliningScope.process(fragment) { fragmentScope ->
                InlineAstVisitor(this, fragmentScope).accept(fragmentScope.allCode)
            }
        }
    }

    fun process(
        inlineFn: InlineFunctionDefinition,
        call: JsInvocation?,
        definitionFragment: JsProgramFragment,
        callsiteScope: InliningScope
    ) {
        // Old fragments are already processed
        if (definitionFragment !in translationResult.newFragments) return

        cycleReporter.processInlineFunction(inlineFn.fn, call) {
            val (fn, wrapperBody) = inlineFn.fn

            if (wrapperBody != null) {
                ImportIntoWrapperInliningScope.process(wrapperBody, definitionFragment) { scope ->
                    InlineAstVisitor(this, scope).accept(wrapperBody)
                }
            } else {
                // e.g. lambda in a non-inline context
                InlineAstVisitor(this, callsiteScope).accept(fn)
            }
        }
    }

    fun inline(scope: InliningScope, call: JsInvocation, currentStatement: JsStatement?): InlineableResult {
        val definition = functionDefinitionLoader.getFunctionDefinition(call, scope)

        val function = scope.importFunctionDefinition(definition)

        val inliningContext = InliningContext(currentStatement)

        val (inlineableBody, resultExpression) = FunctionInlineMutator.getInlineableCallReplacement(call, function, inliningContext)

        // body of inline function can contain call to lambdas that need to be inlined
        InlineAstVisitor(this, scope).accept<JsNode?>(inlineableBody)

        // TODO shouldn't we process the resultExpression qualifier along with the lambda inlining?
        resultExpression?.synthetic = true

        return InlineableResult(JsBlock(inliningContext.previousStatements + inlineableBody), resultExpression)
    }
}