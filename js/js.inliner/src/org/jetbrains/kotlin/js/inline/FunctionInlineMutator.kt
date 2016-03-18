/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.jetbrains.kotlin.js.inline

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.clean.removeDefaultInitializers
import org.jetbrains.kotlin.js.inline.context.InliningContext
import org.jetbrains.kotlin.js.inline.context.NamingContext
import org.jetbrains.kotlin.js.inline.util.*
import org.jetbrains.kotlin.js.inline.util.rewriters.ReturnReplacingVisitor

class FunctionInlineMutator
private constructor(
        private val call: JsInvocation,
        private val inliningContext: InliningContext
) {
    private val invokedFunction: JsFunction
    private val namingContext: NamingContext
    private val body: JsBlock
    private var resultExpr: JsExpression? = null
    private var breakLabel: JsLabel? = null
    private val currentStatement = inliningContext.statementContext.currentNode

    init {

        val functionContext = inliningContext.functionContext
        invokedFunction = functionContext.getFunctionDefinition(call)
        body = invokedFunction.body.deepCopy()
        namingContext = inliningContext.newNamingContext()
    }

    private fun process() {
        val arguments = getArguments()
        val parameters = getParameters()

        replaceThis()
        removeDefaultInitializers(arguments, parameters, body)
        aliasArgumentsIfNeeded(namingContext, arguments, parameters)
        renameLocalNames(namingContext, invokedFunction)
        removeStatementsAfterTopReturn()
        processReturns()

        namingContext.applyRenameTo(body)
        resultExpr = resultExpr?.let {
            namingContext.applyRenameTo(it) as JsExpression
        }
    }

    private fun replaceThis() {
        if (!hasThisReference(body)) return

        var thisReplacement = getThisReplacement(call)
        if (thisReplacement == null || thisReplacement is JsLiteral.JsThisRef) return

        if (thisReplacement.needToAlias()) {
            val thisName = namingContext.getFreshName(getThisAlias())
            namingContext.newVar(thisName, thisReplacement)
            thisReplacement = thisName.makeRef()
        }

        replaceThisReference(body, thisReplacement)
    }

    private fun removeStatementsAfterTopReturn() {
        val statements = body.statements

        val returnIndex = statements.indexOfFirst { it is JsReturn }
        if (returnIndex >= 0) {
            statements.subList(returnIndex + 1, statements.size).clear()
        }
    }

    private fun processReturns() {
        val resultReference = getResultReference()
        if (resultReference != null) {
            resultExpr = resultReference
        }
        assert(resultExpr == null || resultExpr is JsNameRef)

        val breakName = namingContext.getFreshName(getBreakLabel())
        val breakLabel = JsLabel(breakName)
        breakLabel.synthetic = true
        this.breakLabel = breakLabel

        val visitor = ReturnReplacingVisitor(resultExpr as? JsNameRef, breakName.makeRef())
        visitor.accept(body)
    }

    private fun getResultReference(): JsNameRef? {
        if (!isResultNeeded(call)) return null

        val resultName = namingContext.getFreshName(getResultLabel())
        namingContext.newVar(resultName, null)
        return resultName.makeRef()
    }

    private fun getArguments(): List<JsExpression> {
        val arguments = call.arguments
        if (isCallInvocation(call)) {
            return arguments.subList(1, arguments.size)
        }

        return arguments
    }

    private fun isResultNeeded(call: JsInvocation): Boolean {
        return currentStatement !is JsExpressionStatement || call != currentStatement.expression
    }

    private fun getParameters(): List<JsParameter> {
        return invokedFunction.parameters
    }

    private fun getResultLabel(): String {
        return getLabelPrefix() + "result"
    }

    private fun getBreakLabel(): String {
        return getLabelPrefix() + "break"
    }

    private fun getThisAlias(): String {
        return "\$this"
    }

    fun getLabelPrefix(): String {
        val ident = getSimpleIdent(call)
        val labelPrefix = ident ?: "inline$"

        if (labelPrefix.endsWith("$")) {
            return labelPrefix
        }

        return labelPrefix + "$"
    }

    companion object {

        @JvmStatic fun getInlineableCallReplacement(call: JsInvocation, inliningContext: InliningContext): InlineableResult {
            val mutator = FunctionInlineMutator(call, inliningContext)
            mutator.process()

            var inlineableBody: JsStatement = mutator.body
            val breakLabel = mutator.breakLabel
            if (breakLabel != null) {
                breakLabel.statement = inlineableBody
                inlineableBody = breakLabel
            }

            return InlineableResult(inlineableBody, mutator.resultExpr)
        }

        @JvmStatic
        private fun getThisReplacement(call: JsInvocation): JsExpression? {
            if (isCallInvocation(call)) {
                return call.arguments[0]
            }

            if (hasCallerQualifier(call)) {
                return getCallerQualifier(call)
            }

            return null
        }

        private fun hasThisReference(body: JsBlock): Boolean {
            val thisRefs = collectInstances(JsLiteral.JsThisRef::class.java, body)
            return !thisRefs.isEmpty()
        }
    }
}
