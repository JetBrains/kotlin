/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.forcedReturnVariable
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.context.FunctionContext
import org.jetbrains.kotlin.js.inline.context.InliningContext
import org.jetbrains.kotlin.js.inline.util.FunctionWithWrapper
import org.jetbrains.kotlin.js.inline.util.getImportTag
import org.jetbrains.kotlin.js.translate.declaration.transformSpecialFunctionsToCoroutineMetadata
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

class InlinerImpl(
    val existingNameBindings: MutableMap<JsName, String>,
    val existingImports: MutableMap<String, JsName>,
    val inlineFunctionDepth: Int,
    val dfsController: InlineDfsController,
    val inverseNameBindings: Map<JsName, String>,
    val functionContext: FunctionContext,
    val addPrevious: (JsStatement) -> Unit
) : JsVisitorWithContextImpl() {
    val inlinedModuleAliases = HashSet<JsName>()

    val replacementsInducedByWrappers: MutableMap<JsFunction, Map<JsName, JsNameRef>> = mutableMapOf()

    val additionalNameBindings = ArrayList<JsNameBinding>()

    val additionalImports = mutableListOf<Triple<String, JsExpression, JsName>>()

    override fun visit(function: JsFunction, context: JsContext<*>): Boolean {
        val functionWithWrapper = dfsController.functionsByFunctionNodes[function]
        if (functionWithWrapper != null) {
            visit(functionWithWrapper)
            return false
        } else {
            dfsController.startFunction(function)
            return super.visit(function, context)
        }
    }

    override fun endVisit(function: JsFunction, context: JsContext<*>) {
        super.endVisit(function, context)
        if (!dfsController.functionsByFunctionNodes.containsKey(function)) {
            dfsController.endFunction(function)
        }
    }

    override fun visit(x: JsBlock, ctx: JsContext<*>): Boolean {
        val functionWithWrapper = dfsController.functionsByWrapperNodes[x]
        if (functionWithWrapper != null) {
            visit(functionWithWrapper)
            return false
        }
        return super.visit(x, ctx)
    }

    private fun visit(functionWithWrapper: FunctionWithWrapper) {
        dfsController.startFunction(functionWithWrapper.function)

        val wrapperBody = functionWithWrapper.wrapperBody
        if (wrapperBody != null) {
            val existingImports = HashMap<String, JsName>()

            for (statement in wrapperBody.statements) {
                if (statement is JsVars) {
                    val tag = getImportTag(statement)
                    if (tag != null) {
                        existingImports[tag] = statement.vars[0].name
                    }
                }
            }

            val additionalStatements = mutableListOf<JsStatement>()
            val innerInliner = InlinerImpl(
                existingNameBindings,
                existingImports,
                inlineFunctionDepth + 1,
                dfsController,
                inverseNameBindings,
                functionContext
            ) {
                additionalStatements.add(it)
            }
            for (statement in wrapperBody.statements) {
                if (statement !is JsReturn) {
                    innerInliner.acceptStatement(statement)
                } else {
                    innerInliner.accept((statement.expression as JsFunction).body)
                }
            }

            val importStatements = innerInliner.additionalImports.map { (_, importExpr, name) ->
                JsAstUtils.newVar(name, importExpr)
            }

            // TODO keep order
            wrapperBody.statements.addAll(0, importStatements + additionalStatements)
        } else {
            accept(functionWithWrapper.function.body)
        }

        dfsController.endFunction(functionWithWrapper.function)
    }

    override fun visit(call: JsInvocation, context: JsContext<*>): Boolean {
        if (!hasToBeInlined(call)) return true

        if (dfsController.visitCall(call)) {
            val definition = functionContext.getFunctionDefinition(call)

            for (i in 0 until call.arguments.size) {
                val argument = call.arguments[i]
                call.arguments[i] = accept(argument)
            }
            visit(definition)

            return false
        }

        return true
    }

    override fun endVisit(x: JsInvocation, ctx: JsContext<JsNode>) {
        if (hasToBeInlined(x)) {
            inline(x, ctx)
        }

        dfsController.endVisit(x)
    }

    override fun doAcceptStatementList(statements: MutableList<JsStatement>) {
        var i = 0

        while (i < statements.size) {
            val additionalStatements =
                ExpressionDecomposer.preserveEvaluationOrder(statements[i]) { node ->
                    node is JsInvocation && hasToBeInlined(
                        node
                    )
                }
            statements.addAll(i, additionalStatements)
            i += additionalStatements.size + 1
        }

        super.doAcceptStatementList(statements)
    }

    private fun inline(call: JsInvocation, context: JsContext<JsNode>) {
        var functionWithWrapper = functionContext.getFunctionDefinition(call)

        // Since we could get functionWithWrapper as a simple function directly from staticRef (which always points on implementation)
        // we should check if we have a known wrapper for it
        dfsController.functionsByFunctionNodes[functionWithWrapper.function]?.let {
            functionWithWrapper = it
        }

        val function = functionWithWrapper.function.deepCopy()
        function.body = transformSpecialFunctionsToCoroutineMetadata(function.body)
        if (functionWithWrapper.wrapperBody != null) {
            applyWrapper(
                functionWithWrapper.wrapperBody!!,
                function,
                functionWithWrapper.function,
                inlineFunctionDepth,
                replacementsInducedByWrappers,
                existingImports,
                additionalImports,
                existingNameBindings,
                additionalNameBindings,
                inlinedModuleAliases,
                inverseNameBindings
            ) {
                addPrevious(accept(it))
            }
        }

        val inliningContext = InliningContext(lastStatementLevelContext)

        val inlineableResult =
            FunctionInlineMutator.getInlineableCallReplacement(call, function, inliningContext)

        val inlineableBody = inlineableResult.inlineableBody
        var resultExpression = inlineableResult.resultExpression
        val statementContext = inliningContext.statementContext
        // body of inline function can contain call to lambdas that need to be inlined
        val inlineableBodyWithLambdasInlined = accept(inlineableBody)
        assert(inlineableBody === inlineableBodyWithLambdasInlined)

        // Support non-local return from secondary constructor
        // Returns from secondary constructors should return `$this` object.
        // TODO This seems brittle
        val currentFunction = dfsController.currentNamedFunction
        if (currentFunction != null) {
            val returnVariable = currentFunction.forcedReturnVariable
            if (returnVariable != null) {
                inlineableBody.accept(object : RecursiveJsVisitor() {
                    override fun visitReturn(x: JsReturn) {
                        x.expression = returnVariable.makeRef()
                    }
                })
            }
        }

        statementContext.addPrevious(JsAstUtils.flattenStatement(inlineableBody))

        /*
         * Assumes, that resultExpression == null, when result is not needed.
         * @see FunctionInlineMutator.isResultNeeded()
         */
        if (resultExpression == null) {
            statementContext.removeMe()
            return
        }

        resultExpression = accept(resultExpression)
        resultExpression.synthetic = true
        context.replaceMe(resultExpression)
    }

    private fun hasToBeInlined(call: JsInvocation): Boolean {
        val strategy = call.inlineStrategy
        return if (strategy == null || !strategy.isInline) false else functionContext.hasFunctionDefinition(call)
    }
}