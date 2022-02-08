/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestInferApplier {
    private val resolutions = resolve(data)
    private val containers = containersOf(data)

    private val typeAdapter = object : TypeAdapter<FunctionType> {
        override fun declaredSchemaOf(type: FunctionType): Scheme = type.toScheme()
        override fun currentInferredSchemeOf(type: FunctionType): Scheme? = null
        override fun updatedInferredScheme(type: FunctionType, scheme: Scheme) { }
    }

    private val nodeAdapter = object : NodeAdapter<FunctionType, Node> {
        override fun containerOf(node: Node) =
            containers[node]
                ?: (node as? ResolvedParameter)?.let { containers[it.parameter] }
                ?: (node as? ResolvedVariable)?.let { containers[it.variable] }
                ?: (node as? ResolvedExpression)?.let { containers[it.node] }
                ?: error("Could not find container for $node")
        override fun kindOf(node: Node): NodeKind = when (node) {
            is Function -> NodeKind.Function
            is Lambda -> NodeKind.Lambda
            is ResolvedParameter -> NodeKind.ParameterReference
            is ResolvedVariable -> NodeKind.Variable
            is ResolvedExpression -> kindOf(node.node)
            else -> NodeKind.Expression
        }
        override fun schemeParameterIndexOf(node: Node, container: Node): Int {
            val parameter = node as? ResolvedParameter ?: return -1
            val type = typeOf(container) ?: return -1
            val parameters = type.parameters.filter { it.type is FunctionType }
            return parameters.indexOf(parameter.parameter)
        }
        override fun typeOf(node: Node): FunctionType? = when (node) {
            is Function -> node.type
            is Lambda -> node.type
            is ResolvedParameter -> node.type as? FunctionType
            is ResolvedExpression -> node.type as? FunctionType
            is ResolvedVariable -> node.type as? FunctionType
            else -> null
        }

        override fun referencedContainerOf(node: Node): Node? =
            ((node as? ResolvedExpression)?.node as? Function)?.let {
                if (it.type.isOpen || it.type.isBound) null else it
            }
    }

    private fun lazySchemeStorage() = object : LazySchemeStorage<Node> {
        private val map = mutableMapOf<Node, LazyScheme>()
        override fun getLazyScheme(node: Node): LazyScheme? = map[node.storageNode()]
        override fun storeLazyScheme(node: Node, value: LazyScheme) {
            map[node.storageNode()] = value
        }
    }

    private fun errorReporter(): Pair<ErrorReporter<Node>, List<Call>> {
        val errors = mutableListOf<Call>()
        return object : ErrorReporter<Node> {
            override fun reportCallError(node: Node, expected: String, received: String) {
                val call = node as? Call ?: (node as? ResolvedExpression)?.node as? Call ?: return
                errors.add(call)
            }

            override fun reportParameterError(
                node: Node,
                index: Int,
                expected: String,
                received: String
            ) {
                val call = node as? Call ?: (node as? ResolvedExpression)?.node as? Call ?: return
                errors.add(call)
            }

            override fun log(node: Node?, message: String) {
                // Log messages indicate internal error conditions that should be reported by the
                // to a log

                // No log messages should occur during tests.
                error(message)
            }
        } to errors
    }

    private fun findNode(vararg route: String): Node {
        val function = data[route.first()] ?: error("Could not find ${route.first()}")
        var current: Node = function
        for (part in route.drop(1)) {
            val previous = current
            walkChildren(
                current,
                object : EmptyVisitor() {
                    override fun visit(call: Call) {
                        val target = call.target
                        if (target is Ref && target.name == part) current = call
                    }
                    override fun visit(lambda: Lambda) {
                        walkChildren(lambda, this)
                    }
                }
            )
            if (current == previous) error("$part not found")
        }
        return current
    }

    private fun expectCorrectInference(findScheme: (Node) -> Scheme) {
        val expectations = mutableListOf<String>()
        val results = mutableListOf<String>()
        fun expect(name: String, schemeText: String) {
            val function = data[name] ?: error("Could not find $name")
            val scheme = findScheme(function)
            expectations.add("$name: $schemeText")
            results.add("$name: $scheme")
        }

        expect("CoreText", "[UI]")
        expect("BasicText", "[UI]")
        expect("Text", "[UI]")
        expect("Circle", "[Vector]")
        expect("Square", "[Vector]")
        expect("Provider", "[0, [0]]")
        expect("Row", "[UI, [UI]]")
        expect("Button", "[UI, [UI]]")
        expect("Layer", "[Vector, [Vector]]")
        expect("Drawing", "[UI, [Vector]]")
        expect("SimpleOpen", "[_]")
        expect("OpenRecursive", "[_]")
        expect("ClosedRecursive", "[UI]")
        expect("OpenIndirectRecursive", "[_]")
        expect("p1", "[UI]")
        expect("p2", "[Vector]")
        expect("p3", "[UI]")
        expect("p4", "[UI]")
        expect("p5", "[UI]")
        expect("p6", "[Vector]")
        expect("p7", "[UI]")
        expect("p8", "[UI]")
        expect("p9", "[UI, [UI, [UI]]]")
        expect("useVar", "[0, [0]]")
        expect("useIdentity", "[0, [0]]")
        expect("useRun", "[UI, [UI]]")
        expect("useVarAndIdentity", "[UI, [UI], [Vector]]")

        assertEquals(expectations.joinToString("\n"), results.joinToString("\n"))
    }

    fun resolve(resolvedType: ResolvedType): Node =
        when (resolvedType.node) {
            is Lambda -> resolvedType.node
            is Function -> if ((resolvedType.type as FunctionType).isBound) {
                ResolvedExpression(resolvedType.node, resolvedType.type)
            } else resolvedType.node
            is Parameter -> ResolvedParameter(resolvedType.node, resolvedType.type)
            is Variable -> ResolvedVariable(resolvedType.node, resolvedType.type)
            else -> ResolvedExpression(resolvedType.node, resolvedType.type)
        }

    fun resolve(node: Node): Node =
        resolve(resolutions.nodes[node] ?: error("$node was not resolved"))

    private fun dataVisitor(inferApplier: ApplierInferencer<FunctionType, Node>): Visitor =
        object : EmptyVisitor() {
            override fun visit(call: Call) {
                val resolvedCall = resolutions.calls[call] ?: error("Could not resolve $call")
                val target = resolve(resolvedCall.target)
                val arguments = resolvedCall.arguments.mapNotNull { resolvedType ->
                    if (resolvedType.type is FunctionType) resolve(resolvedType)
                    else null
                }
                val resolvedCallNode = resolve(call)
                inferApplier.visitCall(resolvedCallNode, target, arguments)
            }

            override fun visit(variable: Variable) {
                val target = resolve(variable)
                val initializer = resolve(variable.initializer)

                inferApplier.visitVariable(target, initializer)
            }
        }

    @Test
    fun prefixOrderWalk() {
        val (errorReporter, errors) = errorReporter()
        val lazySchemeStorage = lazySchemeStorage()
        val inferApplier = ApplierInferencer(
            typeAdapter,
            nodeAdapter,
            lazySchemeStorage,
            errorReporter
        )

        val visitor = dataVisitor(inferApplier)

        walkData(visitor)

        expectCorrectInference {
            inferApplier.toFinalScheme(it)
        }

        assertTrue(
            "Expected Circle in e1 to be in error",
            findNode("e1", "Circle") in errors
        )
        assertTrue(
            "Expected Circle in e2 to be in error",
            findNode("e2", "Provider", "Circle") in errors
        )
        assertEquals("Unexpected errors reported", 2, errors.size)
    }

    @Test
    fun randomOrderWalk() {
        val (errorReporter, _) = errorReporter()
        val lazySchemeStorage = lazySchemeStorage()
        val inferApplier = ApplierInferencer(
            typeAdapter,
            nodeAdapter,
            lazySchemeStorage,
            errorReporter
        )
        val visitor = dataVisitor(inferApplier)

        randomlyWalkData(visitor)

        expectCorrectInference {
            inferApplier.toFinalScheme(it)
        }

        // Randomly walking the tree produces random errors as well for trees that can be interpreted multiple ways
        // such as data contains, so don't assert on the errors.
    }
}

private class ResolvedParameter(val parameter: Parameter, val type: Type) : Leaf() {
    override fun accept(visitor: Visitor) = visitor.visit(parameter)
    override fun storageNode(): Node = parameter
}

private class ResolvedVariable(val variable: Variable, val type: Type) : Leaf() {
    override fun accept(visitor: Visitor) = visitor.visit(variable)
    override fun storageNode(): Node = variable
}

private class ResolvedExpression(val node: Node, val type: Type) : Leaf() {
    override fun accept(visitor: Visitor) { }
    override fun storageNode(): Node = node
}