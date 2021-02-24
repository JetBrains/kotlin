/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.dce

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.dce.Context.Node
import org.jetbrains.kotlin.js.inline.util.collectLocalVariables

class ReachabilityTracker(
        private val context: Context,
        private val analysisResult: AnalysisResult,
        private val logConsumer: ((DCELogLevel, String) -> Unit)?
) : RecursiveJsVisitor() {
    companion object {
        private val CALL_FUNCTIONS = setOf("call", "apply")
    }

    init {
        context.clearVisited()
    }

    private var currentNodeWithLocation: JsNode? = null
    private var depth = 0
    private val reachableNodesImpl = mutableListOf<Node>()

    val reachableNodes: Iterable<Node> get() = reachableNodesImpl

    override fun visit(x: JsVars.JsVar) {
        if (shouldTraverse(x)) {
            super.visit(x)
        }
    }

    override fun visitExpressionStatement(x: JsExpressionStatement) {
        if (shouldTraverse(x)) {
            super.visitExpressionStatement(x)
        }
    }

    override fun visitReturn(x: JsReturn) {
        if (shouldTraverse(x)) {
            super.visitReturn(x)
        }
    }

    private fun shouldTraverse(x: JsNode): Boolean =
            analysisResult.nodeMap[x] == null && x !in analysisResult.astNodesToEliminate

    override fun visitNameRef(nameRef: JsNameRef) {
        if (visitNameLikeNode(nameRef)) {
            super.visitNameRef(nameRef)
        }
    }

    override fun visitArrayAccess(x: JsArrayAccess) {
        if (visitNameLikeNode(x)) {
            super.visitArrayAccess(x)
        }
    }

    private fun visitNameLikeNode(x: JsExpression): Boolean {
        if (x in analysisResult.astNodesToSkip) return false

        val node = context.extractNode(x)
        if (node != null) {
            if (!node.reachable) {
                reportAndNest("reach: referenced name $node", currentNodeWithLocation) {
                    reach(node)
                    currentNodeWithLocation?.let { node.addUsedByAstNode(it) }
                }
            }
            return false
        }
        else {
            return true
        }
    }

    override fun visitInvocation(invocation: JsInvocation) {
        val function = invocation.qualifier
        when {
            function is JsFunction && function in analysisResult.functionsToEnter -> {
                accept(function.body)
                for (argument in invocation.arguments.filter { it is JsFunction && it in analysisResult.functionsToEnter }) {
                    accept(argument)
                }
            }
            invocation in analysisResult.invocationsToSkip -> {}
            else -> {
                val node = context.extractNode(invocation.qualifier)
                if (node != null && node.memberName in CALL_FUNCTIONS) {
                    val parent = node.parent!!
                    reach(parent)
                    currentNodeWithLocation?.let { parent.addUsedByAstNode(it) }
                }
                super.visitInvocation(invocation)
            }
        }
    }

    override fun visitFunction(x: JsFunction) {
        if (x !in analysisResult.functionsToEnter) {
            x.collectLocalVariables().let {
                context.addNodesForLocalVars(it)
                context.namesOfLocalVars += it
            }
            withErasedThis {
                super.visitFunction(x)
            }
        }
        else {
            super.visitFunction(x)
        }
    }

    private fun withErasedThis(action: () -> Unit) {
        val oldThis = context.thisNode
        context.thisNode = null
        action()
        context.thisNode = oldThis
    }

    override fun visitBreak(x: JsBreak) { }

    override fun visitContinue(x: JsContinue) { }

    fun reach(node: Node) {
        if (node.reachable) return
        node.reachable = true
        if (context.visit(node)) {
            reachableNodesImpl += node
        }

        reachDeclaration(node)

        reachDependencies(node)
        node.members.toList().forEach { (name, member) ->
            if (!member.reachable) {
                reportAndNest("reach: member $name", null) { reach(member) }
            }
        }

        if (node !in analysisResult.functionsToSkip) {
            for (expr in node.functions) {
                reportAndNest("traverse: function", expr) {
                    expr.collectLocalVariables().let {
                        context.addNodesForLocalVars(it)
                        context.namesOfLocalVars += it
                    }
                    withErasedThis { expr.body.accept(this) }
                }
            }
        }

        for (expr in node.expressions) {
            reportAndNest("traverse: value", expr) {
                expr.accept(this)
            }
        }
    }

    private fun reachDependencies(node: Node) {
        val path = mutableListOf<String>()
        var current = node
        while (true) {
            for (ancestorDependency in current.dependencies) {
                if (current in generateSequence(ancestorDependency) { it.parent }) continue
                val dependency = path.asReversed().fold(ancestorDependency) { n, memberName -> n.member(memberName) }
                if (!dependency.reachable) {
                    reportAndNest("reach: dependency $dependency", null) { reach(dependency) }
                }
            }
            path += current.memberName ?: break
            current = current.parent!! // memberName != null => parent != null
        }
    }

    private fun reachDeclaration(node: Node) {
        if (node.hasSideEffects && !node.reachable) {
            reportAndNest("reach: because of side effect", null) {
                reach(node)
            }
        }
        else if (!node.declarationReachable) {
            node.declarationReachable = true
            if (context.visit(node)) {
                reachableNodesImpl += node
            }

            node.original.parent?.let {
                reportAndNest("reach-decl: parent $it", null) {
                    reachDeclaration(it)
                }
            }

            for (expr in node.expressions) {
                reportAndNest("traverse: value", expr) {
                    expr.accept(this)
                }
            }
        }
    }

    override fun visitPrefixOperation(x: JsPrefixOperation) {
        if (x.operator == JsUnaryOperator.TYPEOF) {
            val arg = x.arg
            context.extractNode(arg)?.let {
                reachDeclaration(it)
                return
            }
        }
        super.visitPrefixOperation(x)
    }

    override fun visitElement(node: JsNode) {
        if (node in analysisResult.astNodesToSkip) return
        val newLocation = node.extractLocation()
        val old = currentNodeWithLocation
        if (newLocation != null) {
            currentNodeWithLocation = node
        }
        super.visitElement(node)
        currentNodeWithLocation = old
    }

    private fun reportAndNest(message: String, dueTo: JsNode?, action: () -> Unit) {
        if (logConsumer != null) {
            val indent = "  ".repeat(depth)
            val fullMessage = when (val location = dueTo?.extractLocation()) {
                null -> "$indent$message"
                else -> "$indent$message (due to ${location.asString()})"
            }
            logConsumer.invoke(DCELogLevel.INFO, fullMessage)
        }
        nested(action)
    }

    private fun nested(action: () -> Unit) {
        depth++
        action()
        depth--
    }
}