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

import com.google.common.collect.LinkedHashMultimap
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.js.backend.ast.metadata.specialFunction
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.array
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.index

class Context {
    // Collections per Node consumes too much RAM
    private val nodeDependencies = LinkedHashMultimap.create<Node, Node>()
    private val nodeExpressions = LinkedHashMultimap.create<Node, JsExpression>()
    private val nodeFunctions = LinkedHashMultimap.create<Node, JsFunction>()
    private val nodeUsedByAstNodes = LinkedHashMultimap.create<Node, JsNode>()

    val globalScope = Node()
    val moduleExportsNode = globalScope.member("module").member("exports")
    var currentModule = globalScope
    val nodes = mutableMapOf<JsName, Node>()
    var thisNode: Node? = globalScope
    val namesOfLocalVars = mutableSetOf<JsName>()

    fun addNodesForLocalVars(names: Collection<JsName>) {
        nodes += names.filter { it !in nodes }.associate { it to Node(it) }
    }

    fun markSpecialFunctions(root: JsNode) {
        val candidates = mutableMapOf<JsName, SpecialFunction>()
        val unsuitableNames = mutableSetOf<JsName>()
        val assignedNames = mutableSetOf<JsName>()
        root.accept(object : RecursiveJsVisitor() {
            override fun visit(x: JsVars.JsVar) {
                val name = x.name
                if (!assignedNames.add(name)) {
                    unsuitableNames += name
                }

                val initializer = x.initExpression
                if (initializer != null) {
                    val specialName = when {
                        isDefineInlineFunction(initializer) -> SpecialFunction.DEFINE_INLINE_FUNCTION
                        isWrapFunction(initializer) -> SpecialFunction.WRAP_FUNCTION
                        else -> null
                    }
                    specialName?.let { candidates[name] = specialName }
                }
                super.visit(x)
            }

            override fun visitBinaryExpression(x: JsBinaryOperation) {
                JsAstUtils.decomposeAssignmentToVariable(x)?.let { (left, _) -> unsuitableNames += left }
            }

            override fun visitFunction(x: JsFunction) {
                x.name?.let { unsuitableNames += it }
            }
        })

        for ((name, function) in candidates) {
            if (name !in unsuitableNames) {
                name.specialFunction = function
            }
        }
    }

    fun extractNode(expression: JsExpression): Node? {
        val node = extractNodeImpl(expression)?.original
        return if (node != null && moduleExportsNode in generateSequence(node) { it.parent }) {
            val path = node.pathFromRoot().drop(2)
            path.fold(currentModule.original) { n, memberName -> n.member(memberName) }
        }
        else {
            node
        }
    }

    private fun extractNodeImpl(expression: JsExpression): Node? {
        return when (expression) {
            is JsNameRef -> {
                val qualifier = expression.qualifier
                if (qualifier == null) {
                    val name = expression.name
                    if (name != null) {
                        if (name in namesOfLocalVars) return null
                        nodes[name]?.original?.let { return it }
                    }
                    globalScope.member(expression.ident)
                }
                else {
                    extractNodeImpl(qualifier)?.member(expression.ident)
                }
            }
            is JsArrayAccess -> {
                val index = expression.index
                if (index is JsStringLiteral) extractNodeImpl(expression.array)?.member(index.value) else null
            }
            is JsThisRef -> {
                thisNode
            }
            is JsInvocation -> {
                val qualifier = expression.qualifier
                if (qualifier is JsNameRef && qualifier.qualifier == null && qualifier.ident == "require" &&
                    qualifier.name !in nodes && expression.arguments.size == 1
                ) {
                    val argument = expression.arguments[0]
                    if (argument is JsStringLiteral) {
                        return globalScope.member(argument.value)
                    }
                }
                null
            }
            else -> {
                null
            }
        }
    }

    private var currentColor = 1.toByte()

    fun clearVisited() {
        currentColor++
    }

    fun visit(n: Node) = n.visit(currentColor)

    inner class Node private constructor(val localName: JsName?, parent: Node?, val memberName: String?) {
        private var _membersImpl: MutableMap<String, Node>? = null

        private val membersImpl: MutableMap<String, Node>
            get() = _membersImpl ?: mutableMapOf<String, Node>().also { _membersImpl = it }

        private var rank = 0
        private var hasSideEffectsImpl = false
        private var reachableImpl = false
        private var declarationReachableImpl = false

        val dependencies: Set<Node> get() = nodeDependencies[original]

        val expressions: Set<JsExpression> get() = nodeExpressions[original]

        val functions: Set<JsFunction> get() = nodeFunctions[original]

        val usedByAstNodes: Set<JsNode> get() = nodeUsedByAstNodes[original]

        var hasSideEffects: Boolean
            get() = original.hasSideEffectsImpl
            set(value) {
                original.hasSideEffectsImpl = value
            }

        var reachable: Boolean
            get() = original.reachableImpl
            set(value) {
                original.reachableImpl = value
            }

        var declarationReachable: Boolean
            get() = original.declarationReachableImpl
            set(value) {
                original.declarationReachableImpl = value
            }

        var parent: Node? = parent
            private set

        private var color: Byte = 0

        fun visit(c: Byte): Boolean {
            val result = color != c
            color = c
            return result
        }

        val memberNames: Set<String> get() = original._membersImpl?.keys ?: emptySet()

        constructor(localName: JsName? = null) : this(localName, null, null)

        var original: Node = this
            get() {
                if (field != this) {
                    field = field.original
                }
                return field
            }
            private set

        val members: Map<String, Node> get() = original._membersImpl ?: emptyMap()

        fun addDependency(node: Node) {
            nodeDependencies.put(original, node)
        }

        fun addFunction(function: JsFunction) {
            nodeFunctions.put(original, function)
        }

        fun addExpression(expression: JsExpression) {
            nodeExpressions.put(original, expression)
        }

        fun addUsedByAstNode(node: JsNode) {
            nodeUsedByAstNodes.put(original, node)
        }

        fun member(name: String): Node = original.membersImpl.getOrPut(name) { Node(null, this, name) }.original

        fun alias(other: Node) {
            val a = original
            val b = other.original
            if (a == b) return

            if (a.parent == null && b.parent == null) {
                a.merge(b)
            }
            else if (a.parent == null) {
                if (b.root() == a) a.makeDependencies(b) else b.evacuateFrom(a)
            }
            else if (b.parent == null) {
                if (a.root() == b) a.makeDependencies(b) else a.evacuateFrom(b)
            }
            else {
                a.makeDependencies(b)
            }
        }

        private fun makeDependencies(other: Node) {
            nodeDependencies.put(this, other)
            nodeDependencies.put(other, this)
        }

        private fun evacuateFrom(other: Node) {
            val (existingMembers, newMembers) = other.members.toList().partition { (name, _) -> name in membersImpl }
            other.original = this

            for ((name, member) in newMembers) {
                membersImpl[name] = member
                member.original.parent = this
            }
            for ((name, member) in existingMembers) {
                membersImpl[name]!!.original.merge(member.original)
                membersImpl[name] = member.original
                member.original.parent = this
            }
            other.membersImpl.clear()

            hasSideEffectsImpl = hasSideEffectsImpl || other.hasSideEffectsImpl
            nodeExpressions.putAll(this, nodeExpressions[other])
            nodeFunctions.putAll(this, nodeFunctions[other])
            nodeDependencies.putAll(this, nodeDependencies[other])
            nodeUsedByAstNodes.putAll(this, nodeUsedByAstNodes[other])

            nodeExpressions.removeAll(other)
            nodeFunctions.removeAll(other)
            nodeDependencies.removeAll(other)
            nodeUsedByAstNodes.removeAll(other)
        }

        private fun merge(other: Node) {
            if (this == other) return

            if (rank < other.rank) {
                other.evacuateFrom(this)
            }
            else {
                evacuateFrom(other)
            }

            if (rank == other.rank) {
                rank++
            }
        }

        fun root(): Node = generateSequence(original) { it.parent?.original }.last()

        fun pathFromRoot(): List<String> =
                generateSequence(original) { it.parent?.original }.mapNotNull { it.memberName }
                        .toList().asReversed()

        override fun toString(): String = (root().localName?.ident ?: "<unknown>") + pathFromRoot().joinToString("") { ".$it" }
    }
}
