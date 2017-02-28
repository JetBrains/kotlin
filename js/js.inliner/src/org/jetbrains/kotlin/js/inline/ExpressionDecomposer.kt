/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.inline

import com.intellij.util.SmartList
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects
import org.jetbrains.kotlin.js.backend.ast.metadata.staticRef
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.inline.util.rewriters.ContinueReplacingVisitor
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.*

/**
 * If inline function consists of multiple statements,
 * its statements will be extracted before last visited statement.
 * This action can potentially break evaluation order.
 *
 * An example:
 *      var x = foo() + inlineBox();
 * Inliner could extract statements from inlineBox():
 *      // inlineBox body..
 *      var x = foo() + inlineBox$result;
 * So foo is evaluated before inlineBox() after inline.
 *
 * So we need to extract all nodes, that evaluate before inline call,
 * to temporary vars (preserving evaluation order).
 *
 * For the example, defined before, evaluation order could be preserved this way:
 *      var tmp = foo();
 *      // inlineBox body..
 *      var x = tmp + inlineBox$result;
 *
 * It's desirable to create temporary var only if node can have side effect,
 * and precedes inline call (in JavaScript evaluation order).
 */
internal class ExpressionDecomposer private constructor(
        private val containsExtractable: Set<JsNode>,
        private val containsNodeWithSideEffect: Set<JsNode>
) : JsExpressionVisitor() {

    private var additionalStatements: MutableList<JsStatement> = SmartList()

    companion object {
        @JvmStatic fun preserveEvaluationOrder(statement: JsStatement, canBeExtractedByInliner: (JsNode) -> Boolean): List<JsStatement> {
            val decomposer = with (statement) {
                val extractable = match(canBeExtractedByInliner)
                val containsExtractable = withParentsOfNodes(extractable)
                val nodesWithSideEffect = match { it !is JsLiteral.JsValueLiteral }
                val containsNodeWithSideEffect = withParentsOfNodes(nodesWithSideEffect)

                ExpressionDecomposer(containsExtractable, containsNodeWithSideEffect)
            }

            decomposer.accept(statement)
            return decomposer.additionalStatements
        }
    }

    // TODO: add test case (after KT-7371 fix): var a = foo(), b = foo() + inlineBar()
    override fun visit(x: JsVars, ctx: JsContext<JsNode>): Boolean {
        val vars = x.vars
        var prevVars = SmartList<JsVars.JsVar>()

        for (jsVar in vars) {
            if (jsVar in containsExtractable && prevVars.isNotEmpty()) {
                addStatement(JsVars(prevVars, x.isMultiline))
                prevVars = SmartList<JsVars.JsVar>()
            }

            jsVar.initExpression = accept(jsVar.initExpression)
            prevVars.add(jsVar)
        }

        vars.clear()
        vars.addAll(prevVars)
        return false
    }

    override fun visit(x: JsLabel, ctx: JsContext<JsNode>): Boolean {
        val statement = x.statement
        when (statement) {
            is JsDoWhile -> statement.process(false, x.name)
            is JsWhile -> statement.process(true, x.name)
        }
        return false
    }

    override fun visit(x: JsWhile, ctx: JsContext<JsNode>): Boolean {
        x.process(true, null)
        return false
    }

    override fun visit(x: JsDoWhile, ctx: JsContext<JsNode>): Boolean {
        x.process(false, null)
        return false
    }

    private fun JsWhile.process(addBreakToBegin: Boolean, loopLabel: JsName?) {
        if (test !in containsExtractable) return

        withNewAdditionalStatements {
            test = accept(test)
            val breakIfNotTest = JsIf(not(test), JsBreak())
            // Body can be JsBlock or other JsStatement.
            // Convert to statements list to avoid nested block.
            val bodyStatements = flattenStatement(body)

            if (addBreakToBegin) {
                addStatement(breakIfNotTest)
                addStatements(bodyStatements)
            }
            else {
                // See KT-12275
                val guardName = JsScope.declareTemporaryName("guard\$${(loopLabel?.ident.orEmpty())}")
                val label = JsLabel(guardName).apply { synthetic = true }
                label.statement = JsBlock(bodyStatements)
                addStatements(0, listOf(label))
                ContinueReplacingVisitor(loopLabel, guardName).acceptList(bodyStatements)

                addStatement(breakIfNotTest)
            }

            body = additionalStatements.toStatement()
            test = JsLiteral.TRUE
        }
    }

    // TODO: comma operator?
    override fun visit(x: JsBinaryOperation, ctx: JsContext<JsNode>): Boolean {
        x.arg1 = accept(x.arg1)

        when (x.operator) {
            JsBinaryOperator.AND,
            JsBinaryOperator.OR -> x.processOrAnd(ctx)
            else -> x.process()
        }

        return false
    }

    private fun JsBinaryOperation.processOrAnd(ctx: JsContext<JsNode>) {
        if (arg2 !in containsExtractable) return

        val tmp = Temporary(arg1)
        addStatement(tmp.variable)
        val test = if (operator == JsBinaryOperator.OR) not(tmp.nameRef) else tmp.nameRef
        val arg2Eval = withNewAdditionalStatements {
            arg2 = accept(arg2)
            addStatement(tmp.assign(arg2))
            additionalStatements.toStatement()
        }

        addStatement(JsIf(test, arg2Eval))
        ctx.replaceMe(tmp.nameRef)
    }

    private fun JsBinaryOperation.process() {
        if (arg1 !in containsNodeWithSideEffect || arg2 !in containsExtractable) {
            // If arg1 does not have side effect, but arg2 contains extractable,
            // we should extract from arg2 anyway.
            // If arg2 does not contain extractable, it's still ok to visit.
            arg2 = accept(arg2)
            return
        }

        if (operator.isAssignment) {
            val lhs = arg1
            when (lhs) {
                is JsNameRef -> {
                    lhs.qualifier = lhs.qualifier?.extractToTemporary()
                }
                is JsArrayAccess -> {
                    lhs.array = lhs.array.extractToTemporary()
                }
                else -> {
                    error("Valid JavaScript left-hand side must be either JsNameRef or JsArrayAccess, got: $this")
                }
            }
        }
        else {
            arg1 = arg1.extractToTemporary()
        }

        arg2 = accept(arg2)
    }

    override fun visit(x: JsArrayLiteral, ctx: JsContext<JsNode>): Boolean {
        val elements = x.expressions
        processByIndices(elements, elements.indicesOfExtractable)
        return false
    }

    override fun visit(x: JsArrayAccess, ctx: JsContext<JsNode>): Boolean {
        x.process()
        return false
    }

    private fun JsArrayAccess.process() {
        array = accept(array)

        if (array in containsNodeWithSideEffect && index in containsExtractable) {
            array = array.extractToTemporary()
        }

        index = accept(index)
    }

    override fun visit(x: JsConditional, ctx: JsContext<JsNode>): Boolean {
        x.process(ctx)
        return false
    }

    private fun JsConditional.process(ctx: JsContext<JsNode>) {
        test = accept(test)
        if (then !in containsExtractable && otherwise !in containsExtractable) return

        val tmp = Temporary()
        addStatement(tmp.variable)

        val thenBlock = withNewAdditionalStatements {
            then = accept(then)
            addStatement(tmp.assign(then))
            additionalStatements.toStatement()
        }

        val elseBlock = withNewAdditionalStatements {
            otherwise = accept(otherwise)
            addStatement(tmp.assign(otherwise))
            additionalStatements.toStatement()
        }

        val lazyEval = JsIf(test, thenBlock, elseBlock)
        lazyEval.synthetic = true
        addStatement(lazyEval)
        ctx.replaceMe(tmp.nameRef)
    }

    override fun visit(x: JsInvocation, ctx: JsContext<JsNode>): Boolean {
        CallableInvocationAdapter(x).process()
        return false
    }

    override fun visit(x: JsNew, ctx: JsContext<JsNode>): Boolean {
        CallableNewAdapter(x).process()
        return false
    }

    private abstract class Callable(hasArguments: HasArguments) {
        abstract var qualifier: JsExpression
        abstract val applyBindIfNecessary: Boolean
        val arguments = hasArguments.arguments
    }

    private class CallableInvocationAdapter(val invocation: JsInvocation) : Callable(invocation) {
        override var qualifier: JsExpression
            get() = invocation.qualifier
            set(value) { invocation.qualifier = value }
        override val applyBindIfNecessary = true
    }

    private class CallableNewAdapter(val jsnew: JsNew) : Callable(jsnew) {
        override var qualifier: JsExpression
            get() = jsnew.constructorExpression
            set(value) { jsnew.constructorExpression = value }
        override val applyBindIfNecessary = false
    }

    private fun Callable.process() {
        qualifier = accept(qualifier)
        var matchedIndices = arguments.indicesOfExtractable
        if (matchedIndices.isEmpty()) return

        if (qualifier in containsNodeWithSideEffect) {
            val callee = qualifier as? JsNameRef
            val receiver = callee?.qualifier

            // Qualifier might be a reference to lambda property. See KT-7674
            // An exception here is `fn.call()`, which are marked as side effect free. Further recognition of such
            // case in inliner might be quite difficult, so never extract such call (and other calls marked this way).
            if (qualifier.sideEffects == SideEffectKind.PURE && callee != null && receiver != null &&
                receiver in containsNodeWithSideEffect
            ) {
                val receiverTmp = receiver.extractToTemporary()
                callee.qualifier = receiverTmp
            }
            else {
                if (receiver != null && applyBindIfNecessary) {
                    val receiverTmp = receiver.extractToTemporary()
                    val fqn = JsNameRef(callee.ident, receiverTmp).apply {
                        synthetic = true
                        callee.name?.let { sideEffects = it.sideEffects }
                    }
                    qualifier = fqn.extractToTemporary()
                    qualifier = Namer.getFunctionCallRef(qualifier)
                    arguments.add(0, receiverTmp)
                    matchedIndices = matchedIndices.map { it + 1 }
                }
                else {
                    qualifier = qualifier.extractToTemporary()
                }
            }
        }

        processByIndices(arguments, matchedIndices)
    }

    private fun processByIndices(elements: MutableList<JsExpression>, matchedIndices: List<Int>) {
        var prev = 0
        for (curr in matchedIndices) {
            for (i in prev..curr-1) {
                val arg = elements[i]
                if (arg !in containsNodeWithSideEffect) continue

                elements[i] = arg.extractToTemporary()
            }

            elements[curr] = accept(elements[curr])
            prev = curr
        }
    }

    inline
    private fun <T> withNewAdditionalStatements(fn: ()->T): T {
        val backup = additionalStatements
        additionalStatements = SmartList<JsStatement>()
        val result = fn()
        additionalStatements = backup
        return result
    }

    private fun addStatement(statement: JsStatement) =
            additionalStatements.add(statement)

    private fun addStatements(statements: List<JsStatement>) =
            additionalStatements.addAll(statements)

    private fun addStatements(index: Int, statements: List<JsStatement>) =
            additionalStatements.addAll(index, statements)

    private fun JsExpression.extractToTemporary(): JsExpression {
        val tmp = Temporary(this)
        addStatement(tmp.variable)
        return tmp.nameRef
    }

    private inner class Temporary(val value: JsExpression? = null) {
        val name: JsName = JsScope.declareTemporary()

        val variable: JsVars = newVar(name, value).apply {
            synthetic = true
            name.staticRef = value
        }

        val nameRef: JsExpression
            get() = name.makeRef()

        fun assign(value: JsExpression): JsStatement {
            val statement = JsExpressionStatement(assignment(nameRef, value)).apply { synthetic = true }
            return statement
        }
    }

    private val List<JsNode>.indicesOfExtractable: List<Int>
        get() = indices.filter { get(it) in containsExtractable }
}

/**
 * Visits only expressions that can potentially be extracted by [JsInliner],
 * that directly referenced by statements.
 *
 * For example, won't visit [JsBlock] statements, but will visit test expression of [JsWhile].
 */
internal open class JsExpressionVisitor() : JsVisitorWithContextImpl() {

    override fun visit(x: JsBlock, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsTry, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsDebugger, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsFunction, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsObjectLiteral, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsPropertyInitializer, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsProgram, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsParameter, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsCatch, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsBreak, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsContinue, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsCase, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsDefault, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsEmpty, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsLiteral.JsBooleanLiteral, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsLiteral.JsThisRef, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsNullLiteral, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsNumberLiteral, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsRegExp, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsStringLiteral, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsName, ctx: JsContext<JsNode>): Boolean = false

    // TODO: support these
    // Not generated by compiler now, (can be generated in future or used in js() block)
    override fun visit(x: JsForIn, ctx: JsContext<JsNode>): Boolean = false
    override fun visit(x: JsSwitch, ctx: JsContext<JsNode>): Boolean = false

    // Compiler generates restricted version of for,
    // where init and test do not contain inline calls.
    override fun visit(x: JsFor, ctx: JsContext<JsNode>): Boolean = false

    override fun visit(x: JsIf, ctx: JsContext<JsNode>): Boolean {
        val test = x.ifExpression
        x.ifExpression = accept(test)
        return false
    }

    override fun visit(x: JsWhile, ctx: JsContext<JsNode>): Boolean {
        x.test = accept(x.test)
        return false
    }

    override fun visit(x: JsDoWhile, ctx: JsContext<JsNode>): Boolean {
        x.test = accept(x.test)
        return false
    }

    override fun visit(x: JsLabel, ctx: JsContext<JsNode>): Boolean {
        x.statement = accept(x.statement)
        return false
    }

    override fun visit(x: JsArrayAccess, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsArrayLiteral, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsBinaryOperation, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsConditional, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsInvocation, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsNameRef, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsNew, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsVars.JsVar, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsPostfixOperation, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsPrefixOperation, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsExpressionStatement, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsReturn, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsThrow, ctx: JsContext<JsNode>): Boolean = true
    override fun visit(x: JsVars, ctx: JsContext<JsNode>): Boolean = true
}

/**
 * Returns descendants of receiver, matched by [predicate].
 */
private fun JsNode.match(predicate: (JsNode) -> Boolean): Set<JsNode> {
    val visitor = object : JsExpressionVisitor() {
        val matched = IdentitySet<JsNode>()

        override fun <R : JsNode> doTraverse(node: R, ctx: JsContext<JsNode>?) {
            super.doTraverse(node, ctx)

            if (node !in matched && predicate(node)) {
                matched.add(node)
            }
        }
    }

    visitor.accept(this)
    return visitor.matched
}

/**
 * Returns set of nodes, that satisfy transitive closure of `is parent` relation, starting from [nodes].
 */
private fun JsNode.withParentsOfNodes(nodes: Set<JsNode>): Set<JsNode> {
    val visitor = object : JsExpressionVisitor() {
        private val stack = SmartList<JsNode>()
        val matched = IdentitySet<JsNode>()

        override fun <R : JsNode> doTraverse(node: R, ctx: JsContext<JsNode>?) {
            stack.add(node)
            super.doTraverse(node, ctx)

            if (node in nodes) {
                addAllUntilMatchedOrStatement(stack)
            }

            stack.removeAt(stack.lastIndex)
        }

        fun addAllUntilMatchedOrStatement(nodesOnStack: List<JsNode>) {
            for (i in nodesOnStack.lastIndex downTo 0) {
                val currentNode = nodesOnStack[i]
                if (currentNode in matched) break

                matched.add(currentNode)
                if (currentNode is JsStatement) break
            }
        }
    }

    visitor.accept(this)
    return visitor.matched
}

private fun List<JsStatement>.toStatement(): JsStatement =
        when (size) {
            0 -> JsEmpty
            1 -> get(0)
            else -> JsBlock(this)
        }
