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
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.js.backend.ast.metadata.specialFunction
import org.jetbrains.kotlin.js.dce.Context.Node.Companion.toInt
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.array
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.index
import java.util.*

class Context {
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
        return if (node != null && moduleExportsNode in generateSequence(node) { it.qualifier?.parent }) {
            val path = node.pathFromRoot().drop(2)
            path.fold(currentModule.original) { n, memberName -> n.member(memberName) }
        } else {
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
                } else {
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

    class Node private constructor(val localName: JsName?, qualifier: Qualifier?) {
        companion object {
            private const val HAS_SIDE_EFFECT = 1
            private const val REACHABLE = 1 shl 1
            private const val DECLARATION_REACHABLE = 1 shl 2

            private fun Boolean.toInt() = if (this) 1 else 0
        }

        private var _dependenciesImpl: MutableSet<Node>? = null
        private var _expressionsImpl: MutableSet<JsExpression>? = null
        private var _functionsImpl: MutableSet<JsFunction>? = null
        private var _membersImpl: MutableMap<String, Node>? = null
        private var _usedByAstNodesImpl: MutableSet<JsNode>? = null

        private val dependenciesImpl: MutableSet<Node>
            get() = _dependenciesImpl ?: mutableSetOf<Node>().also { _dependenciesImpl = it }
        private val expressionsImpl: MutableSet<JsExpression>
            get() = _expressionsImpl ?: mutableSetOf<JsExpression>().also { _expressionsImpl = it }
        private val functionsImpl: MutableSet<JsFunction>
            get() = _functionsImpl ?: mutableSetOf<JsFunction>().also { _functionsImpl = it }
        private val membersImpl: MutableMap<String, Node>
            get() = _membersImpl ?: mutableMapOf<String, Node>().also { _membersImpl = it }
        private val usedByAstNodesImpl: MutableSet<JsNode>
            get() = _usedByAstNodesImpl ?: mutableSetOf<JsNode>().also { _usedByAstNodesImpl = it }

        private var rank = 0
        private var flags = 0
        private var hasSideEffectsImpl: Boolean
            get() = checkFlag(HAS_SIDE_EFFECT)
            set(value) = setFlag(value, HAS_SIDE_EFFECT)

        private var reachableImpl
            get() = checkFlag(REACHABLE)
            set(value) = setFlag(value, REACHABLE)

        private var declarationReachableImpl
            get() = checkFlag(DECLARATION_REACHABLE)
            set(value) = setFlag(value, DECLARATION_REACHABLE)

        private fun checkFlag(mask: Int): Boolean = (flags and mask) != 0

        private fun setFlag(value: Boolean, mask: Int) {
            flags = flags xor ((-value.toInt() xor flags) and mask)
        }

        val dependencies: MutableSet<Node> get() = original.dependenciesImpl

        val expressions: MutableSet<JsExpression> get() = original.expressionsImpl

        val functions: MutableSet<JsFunction> get() = original.functionsImpl

        val usedByAstNodes: MutableSet<JsNode> get() = original.usedByAstNodesImpl

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

        var qualifier: Qualifier? = qualifier
            private set

        var tag: Int = -1

        val memberNames: MutableSet<String> get() = original.membersImpl.keys

        constructor(localName: JsName? = null) : this(localName, null)

        var original: Node = this
            get() {
                if (field != this) {
                    field = field.original
                }
                return field
            }
            private set

        val members: Map<String, Node> get() = original.membersImpl

        fun member(name: String): Node = original.membersImpl.getOrPut(name) { Node(null, Qualifier(this, name)) }.original

        fun alias(other: Node) {
            val a = original
            val b = other.original
            if (a == b) return

            if (a.qualifier == null && b.qualifier == null) {
                a.merge(b)
            } else if (a.qualifier == null) {
                if (b.root() == a) a.makeDependencies(b) else b.evacuateFrom(a)
            } else if (b.qualifier == null) {
                if (a.root() == b) a.makeDependencies(b) else a.evacuateFrom(b)
            } else {
                a.makeDependencies(b)
            }
        }

        private fun makeDependencies(other: Node) {
            dependenciesImpl += other
            other.dependenciesImpl += this
        }

        private fun evacuateFrom(other: Node) {
            val (existingMembers, newMembers) = other.members.toList().partition { (name, _) -> name in membersImpl }
            other.original = this

            for ((name, member) in newMembers) {
                membersImpl[name] = member
                member.original.qualifier = Qualifier(this, member.original.qualifier!!.memberName)
            }
            for ((name, member) in existingMembers) {
                membersImpl[name]!!.original.merge(member.original)
                membersImpl[name] = member.original
                member.original.qualifier = Qualifier(this, member.original.qualifier!!.memberName)
            }
            other.membersImpl.clear()

            hasSideEffectsImpl = hasSideEffectsImpl || other.hasSideEffectsImpl
            if (!other._expressionsImpl.isNullOrEmpty()) {
                expressionsImpl += other.expressionsImpl
            }
            if (!other._functionsImpl.isNullOrEmpty()) {
                functionsImpl += other.functionsImpl
            }
            if (!other._dependenciesImpl.isNullOrEmpty()) {
                dependenciesImpl += other.dependenciesImpl
            }
            if (!other._usedByAstNodesImpl.isNullOrEmpty()) {
                usedByAstNodesImpl += other.usedByAstNodesImpl
            }

            other._expressionsImpl?.clear()
            other._functionsImpl?.clear()
            other._dependenciesImpl?.clear()
            other._usedByAstNodesImpl?.clear()
        }

        private fun merge(other: Node) {
            if (this == other) return

            if (rank < other.rank) {
                other.evacuateFrom(this)
            } else {
                evacuateFrom(other)
            }

            if (rank == other.rank) {
                rank++
            }
        }

        fun root(): Node = generateSequence(original) { it.qualifier?.parent?.original }.last()

        fun pathFromRoot(): List<String> =
            generateSequence(original) { it.qualifier?.parent?.original }.mapNotNull { it.qualifier?.memberName }
                .toList().asReversed()

        override fun toString(): String = (root().localName?.ident ?: "<unknown>") + pathFromRoot().joinToString("") { ".$it" }
    }

    class Qualifier(val parent: Node, val memberName: String)
}