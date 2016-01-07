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
import com.google.dart.compiler.util.AstUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.js.inline.clean.removeDefaultInitializers
import org.jetbrains.kotlin.js.inline.context.InliningContext
import org.jetbrains.kotlin.js.inline.context.NamingContext
import org.jetbrains.kotlin.js.inline.util.*
import org.jetbrains.kotlin.js.inline.util.rewriters.ReturnReplacingVisitor
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.newVar
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.*

class FunctionInlineMutator
private constructor(
        private val call: JsInvocation,
        private val inliningContext: InliningContext
) {
    private val invokedFunction: JsFunction
    private val isResultNeeded: Boolean
    private val namingContext: NamingContext
    private val body: JsBlock
    private var resultExpr: JsExpression? = null
    private var breakLabel: JsLabel? = null
    private val currentStatement = inliningContext.statementContext.currentNode

    init {

        val functionContext = inliningContext.functionContext
        invokedFunction = functionContext.getFunctionDefinition(call)
        body = invokedFunction.body.deepCopy()
        isResultNeeded = isResultNeeded(call)
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

        val statementsSize = statements.size
        for (i in 0..statementsSize - 1) {
            val statement = statements.get(i)

            if (statement is JsReturn) {
                statements.subList(i + 1, statementsSize).clear()
                break
            }
        }
    }

    private fun processReturns() {
        if (currentStatement is JsReturn && currentStatement.expression === call) {
            inliningContext.statementContext.removeMe()
            return
        }

        val returnCount = collectInstances(JsReturn::class.java, body).size
        if (returnCount == 0) {
            // TODO return Unit (KT-5647)
            resultExpr = JsLiteral.UNDEFINED
            return
        }

        if (returnCount == 1) {
            val statements = body.statements
            val lastTopLevelStatement = statements[statements.lastIndex]

            if (lastTopLevelStatement is JsReturn) {
                resultExpr = lastTopLevelStatement.expression
                statements.removeAt(statements.lastIndex)
                return
            }
        }

        doReplaceReturns()
    }

    private fun doReplaceReturns() {
        val resultReference = getResultReference()
        if (resultReference != null) {
            resultExpr = resultReference
        }
        assert(resultExpr == null || resultExpr is JsNameRef)

        val breakName = namingContext.getFreshName(getBreakLabel())
        breakLabel = JsLabel(breakName)

        val visitor = ReturnReplacingVisitor(resultExpr as? JsNameRef, breakName.makeRef())
        visitor.accept(body)

        val statements = body.statements
        val last = statements.lastOrNull() as? JsBreak

        if (last?.label?.name === breakLabel?.name) {
            statements.removeAt(statements.lastIndex)
        }
    }

    private fun getResultReference(): JsNameRef? {
        if (!isResultNeeded) return null

        val existingReference = when (currentStatement) {
            is JsExpressionStatement -> {
                val expression = currentStatement.expression as? JsBinaryOperation
                expression?.getResultReference()
            }
            is JsVars -> currentStatement.getResultReference()
            else -> null
        }

        if (existingReference != null) return existingReference

        val resultName = namingContext.getFreshName(getResultLabel())
        namingContext.newVar(resultName, null)
        return resultName.makeRef()
    }

    private fun JsBinaryOperation.getResultReference(): JsNameRef? {
        if (operator !== JsBinaryOperator.ASG || arg2 !== call) return null

        return arg1 as? JsNameRef
    }

    private fun JsVars.getResultReference(): JsNameRef? {
        val vars = vars
        val variable = vars.first()

        // var a = expr1 + call() is ok, but we don't want to reuse 'a' for result,
        // as it means to replace every 'return expr2' to 'a = expr1 + expr2'.
        // If there is more than one return, expr1 copies are undesirable.
        if (variable.initExpression !== call || vars.size > 1) return null

        val varName = variable.name
        with (inliningContext.statementContext) {
            removeMe()
            addPrevious(newVar(varName, null))
        }

        return varName.makeRef()
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
                return call.arguments.get(0)
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

        @JvmStatic fun canBeExpression(function: JsFunction): Boolean {
            return canBeExpression(function.body)
        }

        private fun canBeExpression(body: JsBlock): Boolean {
            val statements = body.statements
            return statements.size == 1 && statements.get(0) is JsReturn
        }
    }
}
