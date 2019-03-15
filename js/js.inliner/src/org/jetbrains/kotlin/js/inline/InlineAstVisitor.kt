/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.descriptor
import org.jetbrains.kotlin.js.backend.ast.metadata.forcedReturnVariable
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.backend.ast.metadata.psiElement
import org.jetbrains.kotlin.js.inline.clean.FunctionPostProcessor
import org.jetbrains.kotlin.js.inline.clean.removeUnusedLocalFunctionDeclarations
import org.jetbrains.kotlin.js.inline.util.extractFunction
import org.jetbrains.kotlin.js.inline.util.refreshLabelNames
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

// Visits AST, detects inline function declarations and invocations.
class InlineAstVisitor(
    private val jsInliner: JsInliner,
    private val scope: InliningScope
) : JsVisitorWithContextImpl() {
    override fun visit(x: JsInvocation, ctx: JsContext<*>): Boolean {
        // Is it `defineInlineFunction('tag', ...)`?
        InlineMetadata.decompose(x)?.let {
            jsInliner.process(InlineFunctionDefinition(it.function, it.tag.value), x, scope.fragment, scope)
            return false
        }

        // Is it `wrapFunction(...)`?
        InlineMetadata.tryExtractFunction(x)?.let {
            jsInliner.process(InlineFunctionDefinition(it, null), x, scope.fragment, scope)
            return false
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


    override fun endVisit(x: JsNameRef, ctx: JsContext<in JsNode>) {
        tryCreatePropertyGetterInvocation(x)?.let {
            endVisit(it, ctx)
        }
    }

    override fun endVisit(x: JsBinaryOperation, ctx: JsContext<in JsNode>) {
        tryCreatePropertySetterInvocation(x)?.let {
            endVisit(it, ctx)
        }
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

    override fun doAcceptStatementList(statements: MutableList<JsStatement>) {
        var i = 0

        while (i < statements.size) {
            val additionalStatements = ExpressionDecomposer.preserveEvaluationOrder(statements[i], ::hasToBeInlined)
            statements.addAll(i, additionalStatements)
            i += additionalStatements.size + 1
        }

        super.doAcceptStatementList(statements)
    }

    private fun hasToBeInlined(node: JsNode): Boolean {
        return when (node) {
            is JsInvocation -> hasToBeInlined(node)
            is JsNameRef -> node.inlineStrategy != null && tryCreatePropertyGetterInvocation(node)?.let { hasToBeInlined(it) } ?: false
            is JsBinaryOperation -> node.operator.isAssignment && node.arg1?.let { left ->
                left is JsNameRef && left.inlineStrategy != null && tryCreatePropertySetterInvocation(node)?.let { hasToBeInlined(it) } ?: false
            } ?: false
            else -> false
        }
    }

    private fun hasToBeInlined(call: JsInvocation): Boolean {
        val strategy = call.inlineStrategy
        return if (strategy == null || !strategy.isInline) false else jsInliner.functionDefinitionLoader.hasFunctionDefinition(call, scope)
    }

    private fun patchReturnsFromSecondaryConstructor(function: JsFunction) {
        // Support non-local return from secondary constructor
        // Returns from secondary constructors should return `$this` object.
        function.forcedReturnVariable?.let { returnVariable ->
            function.body.accept(object : RecursiveJsVisitor() {
                override fun visitReturn(x: JsReturn) {
                    x.expression = returnVariable.makeRef()
                }
            })
        }
    }

    private fun tryCreatePropertyGetterInvocation(x: JsNameRef): JsInvocation? {
        if (x.inlineStrategy != null && x.descriptor is PropertyGetterDescriptor) {
            val dummyInvocation = JsInvocation(x)
            copyInlineMetadata(x, dummyInvocation)
            return dummyInvocation
        }
        return null
    }

    private fun tryCreatePropertySetterInvocation(x: JsBinaryOperation): JsInvocation? {
        if (!x.operator.isAssignment || x.arg1 !is JsNameRef) return null
        val name = x.arg1 as JsNameRef
        if (name.inlineStrategy != null && name.descriptor is PropertySetterDescriptor) {
            val dummyInvocation = JsInvocation(name, x.arg2)
            copyInlineMetadata(name, dummyInvocation)
            return dummyInvocation
        }
        return null
    }

    private fun copyInlineMetadata(from: JsNameRef, to: JsInvocation) {
        to.inlineStrategy = from.inlineStrategy
        to.descriptor = from.descriptor
        to.psiElement = from.psiElement
    }
}