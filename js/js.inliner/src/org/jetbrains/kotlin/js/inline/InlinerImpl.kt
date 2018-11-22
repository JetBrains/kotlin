/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.forcedReturnVariable
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.clean.FunctionPostProcessor
import org.jetbrains.kotlin.js.inline.clean.removeUnusedLocalFunctionDeclarations
import org.jetbrains.kotlin.js.inline.context.FunctionContext
import org.jetbrains.kotlin.js.inline.context.InliningContext
import org.jetbrains.kotlin.js.inline.util.refreshLabelNames
import org.jetbrains.kotlin.js.translate.declaration.transformSpecialFunctionsToCoroutineMetadata
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

// TODO stateless?
class InlinerImpl(
    val cycleReporter: InlinerCycleReporter,
    val functionContext: FunctionContext,
    // TODO other way around? Need to find a correct inliner by function declaration.
    val scope: InliningScope
) : JsVisitorWithContextImpl() {

    override fun visit(function: JsFunction, context: JsContext<*>): Boolean {
        functionContext.functionsByFunctionNodes[function]?.let { (function, wrapper) ->
            visit(function, wrapper)
            return false
        }
        visit(function, null)
        return true
    }

    override fun visit(x: JsBlock, ctx: JsContext<*>): Boolean {
        // TODO Seems like a very roundabout way. Probably should reuse same approach as in CoroutineTransformer and ...Splitter
        // TODO That approach might be missing inline properties. Check!
        functionContext.functionsByWrapperNodes[x]?.let { (function, wrapper) ->
            visit(function, wrapper)
            return false
        }
        return super.visit(x, ctx)
    }

    private fun visit(function: JsFunction, wrapperBody: JsBlock?) {
        cycleReporter.startFunction(function)

        // TODO Different visitors?
        if (wrapperBody != null && scope is ProgramFragmentInliningScope) {
            PublicInlineFunctionInliningScope(scope.fragment, cycleReporter, functionContext, function, wrapperBody).process()
        } else {
            // TODO this is still not super-clear
            accept(function.body)
        }

        // Cleanup
        refreshLabelNames(function.body, function.scope)
        removeUnusedLocalFunctionDeclarations(function)
        FunctionPostProcessor(function).apply()

        cycleReporter.endFunction(function)
    }

    override fun endVisit(call: JsInvocation, ctx: JsContext<JsNode>) {
        if (hasToBeInlined(call)) {

            val definition = functionContext.getFunctionDefinition(call, scope)

            if (cycleReporter.shouldProcess(definition.functionWithWrapper, call)) {
                definition.process()
            }

            inline(call, definition, ctx)
        }

        cycleReporter.endVisit(call)
    }

    // TODO This could be extracted into a separate pass.
    // TODO Don't forget to run it on the freshly inlined code!
    override fun doAcceptStatementList(statements: MutableList<JsStatement>) {
        var i = 0

        while (i < statements.size) {
            val additionalStatements = ExpressionDecomposer.preserveEvaluationOrder(statements[i]) { node ->
                node is JsInvocation && hasToBeInlined(node)
            }
            statements.addAll(i, additionalStatements)
            i += additionalStatements.size + 1
        }

        super.doAcceptStatementList(statements)
    }

    // TODO a lot of code... Probably a bad sign
    private fun inline(call: JsInvocation, definition: InlineFunctionDefinition, context: JsContext<JsNode>) {

        // ---------------
        // This should be isolated

        val function = scope.importFunctionDefinition(definition)
        // TODO This should be done inside the importer
        function.body = transformSpecialFunctionsToCoroutineMetadata(function.body)

        // -------------------

        val statementContext = lastStatementLevelContext

        val (inlineableBody, resultExpression) =
                FunctionInlineMutator.getInlineableCallReplacement(call, function, InliningContext(statementContext))

        // body of inline function can contain call to lambdas that need to be inlined
        val inlineableBodyWithLambdasInlined = accept(inlineableBody)
        assert(inlineableBody === inlineableBodyWithLambdasInlined)

        patchReturnsFromSecondaryConstructor(inlineableBody)

        statementContext.addPrevious(JsAstUtils.flattenStatement(inlineableBody))

        /*
         * Assumes, that resultExpression == null, when result is not needed.
         * @see FunctionInlineMutator.isResultNeeded()
         */
        if (resultExpression == null) {
            statementContext.removeMe()
            return
        }

        // TODO Why accept? Seems unnecessary... Some inline call in the qualifier? Shouldn't this be done along with the lambdas then?
        accept(resultExpression)?.let {
            it.synthetic = true
            context.replaceMe(it)
        }
    }

    private fun patchReturnsFromSecondaryConstructor(inlineableBody: JsStatement) {
        // Support non-local return from secondary constructor
        // Returns from secondary constructors should return `$this` object.
        // TODO This seems brittle
        cycleReporter.currentNamedFunction?.forcedReturnVariable?.let { returnVariable ->
            inlineableBody.accept(object : RecursiveJsVisitor() {
                override fun visitReturn(x: JsReturn) {
                    x.expression = returnVariable.makeRef()
                }
            })
        }
    }

    private fun hasToBeInlined(call: JsInvocation): Boolean {
        val strategy = call.inlineStrategy
        return if (strategy == null || !strategy.isInline) false else functionContext.hasFunctionDefinition(call, scope)
    }
}