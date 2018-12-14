/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.forcedReturnVariable
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.inline.clean.FunctionPostProcessor
import org.jetbrains.kotlin.js.inline.clean.removeUnusedLocalFunctionDeclarations
import org.jetbrains.kotlin.js.inline.util.extractFunction
import org.jetbrains.kotlin.js.inline.util.refreshLabelNames
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

// TODO stateless?
class InlinerImpl(
    val jsInliner: JsInliner,
    val scope: InliningScope
) : JsVisitorWithContextImpl() {

    override fun visit(x: JsBinaryOperation, ctx: JsContext<*>): Boolean {
        val assignment = JsAstUtils.decomposeAssignment(x)
        if (assignment != null) {
            val (left, right) = assignment
            if (left is JsNameRef) {
                val name = left.name
                if (name != null) {
                    extractFunction(right)?.let { function ->
                        jsInliner.process(InlineFunctionDefinition(function, null), null, scope)
                    }
                }
            }
        }

        return super.visit(x, ctx)
    }

    override fun visit(x: JsVars.JsVar, ctx: JsContext<*>): Boolean {
        val initializer = x.initExpression
        val name = x.name
        if (initializer != null && name != null) {
            extractFunction(initializer)?.let { function ->
                jsInliner.process(InlineFunctionDefinition(function, null), null, scope)
            }
        }

        return super.visit(x, ctx)
    }

    override fun visit(x: JsInvocation, ctx: JsContext<*>): Boolean {
        InlineMetadata.decompose(x)?.let {
            jsInliner.process(InlineFunctionDefinition(it.function, it.tag.value), x, scope)
        }

        return super.visit(x, ctx)
    }

    override fun visit(x: JsFunction, ctx: JsContext<*>): Boolean {
        return jsInliner.cycleReporter.withFunction(x) {
            super.visit(x, ctx)
        }
    }

    override fun endVisit(function: JsFunction, ctx: JsContext<*>) {
        // Cleanup
        patchReturnsFromSecondaryConstructor(function)
        refreshLabelNames(function.body, function.scope)
        removeUnusedLocalFunctionDeclarations(function)
        FunctionPostProcessor(function).apply()
    }


    override fun endVisit(call: JsInvocation, ctx: JsContext<JsNode>) {
        if (hasToBeInlined(call)) {
            val (inlineableBody, resultExpression) = jsInliner.inline(scope, call, lastStatementLevelContext.currentNode)

            lastStatementLevelContext.addPrevious(JsAstUtils.flattenStatement(inlineableBody))

            // Assumes, that resultExpression == null, when result is not needed.
            // @see FunctionInlineMutator.isResultNeeded()
            if (resultExpression == null) {
                lastStatementLevelContext.removeMe()
            } else {
                ctx.replaceMe(resultExpression)
            }
        }
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


    private fun hasToBeInlined(call: JsInvocation): Boolean {
        val strategy = call.inlineStrategy
        return if (strategy == null || !strategy.isInline) false else jsInliner.functionContext.hasFunctionDefinition(call, scope)
    }

    private fun patchReturnsFromSecondaryConstructor(function: JsFunction) {
        // Support non-local return from secondary constructor
        // Returns from secondary constructors should return `$this` object.
        // TODO This seems brittle
        function.forcedReturnVariable?.let { returnVariable ->
            function.body.accept(object : RecursiveJsVisitor() {
                override fun visitReturn(x: JsReturn) {
                    x.expression = returnVariable.makeRef()
                }
            })
        }
    }
}