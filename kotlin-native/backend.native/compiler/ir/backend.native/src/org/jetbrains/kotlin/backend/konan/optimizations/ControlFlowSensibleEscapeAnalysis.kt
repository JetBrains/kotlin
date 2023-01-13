/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DirectedGraphCondensationBuilder
import org.jetbrains.kotlin.backend.konan.DirectedGraphMultiNode
import org.jetbrains.kotlin.backend.konan.ir.actualCallee
import org.jetbrains.kotlin.backend.konan.ir.isUnconditional
import org.jetbrains.kotlin.backend.konan.ir.isVirtualCall
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.logMultiple
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

private inline fun <reified T : Comparable<T>> Array<T>.sortedAndDistinct(): Array<T> {
    this.sort()
    if (this.isEmpty()) return this
    val unique = mutableListOf(this[0])
    for (i in 1 until this.size)
        if (this[i] != this[i - 1])
            unique.add(this[i])
    return unique.toTypedArray()
}

private fun <T> MutableList<T>.removeAll(element: T) {
    var i = 0
    for (j in indices) {
        val item = this[j]
        if (item != element && j != i)
            this[i++] = item
    }
    while (size != i)
        removeAt(size - 1)
}

private val IrFieldSymbol.name: String get() = if (isBound) owner.name.asString() else "unbound"

internal object ControlFlowSensibleEscapeAnalysis {
    private class InterproceduralAnalysis(val context: Context, val callGraph: CallGraph) {
        fun analyze() {
            // TODO: To common function.
            context.logMultiple {
                +"CALL GRAPH"
                callGraph.directEdges.forEach { (t, u) ->
                    +"    FUN $t"
                    u.callSites.forEach {
                        val label = when {
                            it.isVirtual -> "VIRTUAL"
                            callGraph.directEdges.containsKey(it.actualCallee) -> "LOCAL"
                            else -> "EXTERNAL"
                        }
                        +"        CALLS $label ${it.actualCallee}"
                    }
                    callGraph.reversedEdges[t]!!.forEach { +"        CALLED BY $it" }
                }
                +""
            }

            val condensation = DirectedGraphCondensationBuilder(callGraph).build()

            context.logMultiple {
                +"CONDENSATION"
                condensation.topologicalOrder.forEach { multiNode ->
                    +"    MULTI-NODE"
                    multiNode.nodes.forEach { +"        $it" }
                }
                +""
                +"CONDENSATION(DETAILED)"
                condensation.topologicalOrder.forEach { multiNode ->
                    +"    MULTI-NODE"
                    multiNode.nodes.forEach {
                        +"        $it"
                        callGraph.directEdges[it]!!.callSites
                                .filter { callGraph.directEdges.containsKey(it.actualCallee) }
                                .forEach { +"            CALLS ${it.actualCallee}" }
                        callGraph.reversedEdges[it]!!.forEach { +"            CALLED BY $it" }
                    }
                }
                +""
            }

//            val functions = buildSet {
//                callGraph.directEdges.values.forEach {
//                    add(it.symbol.irFunction)
//                    it.callSites.forEach { callSite -> add(callSite.actualCallee.irFunction) }
//                }
//            }

            val escapeAnalysisResults = mutableMapOf<IrFunctionSymbol, EscapeAnalysisResult>()
            for (multiNode in condensation.topologicalOrder.reversed())
                analyze(multiNode, escapeAnalysisResults)
        }

        private fun analyze(multiNode: DirectedGraphMultiNode<DataFlowIR.FunctionSymbol.Declared>,
                            escapeAnalysisResults: MutableMap<IrFunctionSymbol, EscapeAnalysisResult>) {
            val nodes = multiNode.nodes.toList()

            if (nodes.size != 1)
                return // TODO

            if (callGraph.directEdges[nodes[0]]?.symbol?.irFunction?.fileOrNull?.name?.endsWith("z.kt") != true) return

            context.logMultiple {
                +"Analyzing multiNode:\n    ${nodes.joinToString("\n   ") { it.toString() }}"
                nodes.forEach { from ->
                    +"IR"
                    +(from.irFunction?.dump() ?: "")
                    callGraph.directEdges[from]!!.callSites.forEach { to ->
                        +"CALL"
                        +"   from $from"
                        +"   to ${to.actualCallee}"
                    }
                }
            }

            if (nodes.size == 1)
                intraproceduralAnalysis(callGraph.directEdges[nodes[0]] ?: return, escapeAnalysisResults)
            else {
                TODO()
            }
        }
//        private val returnsValueField = IrFieldSymbolImpl()

        private class EscapeAnalysisResult(val graph: PointsToGraph, val returnValue: Node, val parameters: List<Node.Object>)

        private sealed class Node(var id: Int) {
//            @Suppress("LeakingThis")
//            var mirroredNode = this

            abstract fun shallowCopy(): Node

            open class Object(id: Int) : Node(id) {
                val fields = mutableMapOf<IrFieldSymbol, FieldValue>()

                override fun shallowCopy() = Object(id)

                override fun toString() = "D$id"
            }

            class Parameter(id: Int, val index: Int, val irValueParameter: IrValueParameter) : Object(id) {
                override fun shallowCopy() = Parameter(id, index, irValueParameter)

                override fun toString() = "P$index[${irValueParameter.name}]"
            }

            abstract class Reference(id: Int) : Node(id) {
                val pointsTo = mutableListOf<Object>()
                val assignedWith = mutableListOf<Reference>()
                val assignedTo = mutableListOf<Reference>()
            }

            class FieldValue(id: Int, val field: IrFieldSymbol) : Reference(id) {
                override fun shallowCopy() = FieldValue(id, field)

                override fun toString() = "F$id"
            }

            class Variable(id: Int, val irVariable: IrVariable?) : Reference(id) {
                override fun shallowCopy() = Variable(id, irVariable)

                override fun toString() = "V$id[${irVariable?.name?.asString() ?: "temp"}]"
            }

            object Unit : Object(UNIT_ID) {
                override fun shallowCopy() = this

                override fun toString() = "<U>"
            }

            object Global : Object(GLOBAL_ID) {
                override fun shallowCopy() = this

                override fun toString() = "<G>"
            }

            companion object {
                const val UNIT_ID = -1
                const val GLOBAL_ID = -2
            }
        }

        private inner class PointsToGraphForest {
            private var currentNodeId = 0
            fun nextNodeId() = currentNodeId++
            val totalNodes get() = currentNodeId

            private val fieldIds = mutableMapOf<Pair<Int, IrFieldSymbol>, Int>()
            fun getFieldId(objectId: Int, field: IrFieldSymbol) = fieldIds.getOrPut(Pair(objectId, field)) { nextNodeId() }

            fun cloneGraph(graph: PointsToGraph, otherNodesToMap: MutableList<Node>): PointsToGraph {
                val newNodes = arrayOfNulls<Node?>(totalNodes)
                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
                val variableNodes = mutableMapOf<IrVariable, Node.Variable>()
                for (node in graph.nodes)
                    newNodes[node.id] = node.shallowCopy().also { copy ->
                        (copy as? Node.Parameter)?.irValueParameter?.let { parameterNodes[it] = copy }
                        (copy as? Node.Variable)?.irVariable?.let { variableNodes[it] = copy }
                    }
                for (node in graph.nodes) {
                    when (val newNode = newNodes[node.id]!!) {
                        is Node.Object -> {
                            (node as Node.Object).fields.entries.forEach { (field, fieldNode) ->
                                newNode.fields[field] = newNodes[fieldNode.id] as Node.FieldValue
                            }
                        }
                        is Node.Reference -> {
                            (node as Node.Reference).pointsTo.mapTo(newNode.pointsTo) { newNodes[it.id] as Node.Object }
                            node.assignedWith.mapTo(newNode.assignedWith) { newNodes[it.id] as Node.Reference }
                            node.assignedTo.mapTo(newNode.assignedTo) { newNodes[it.id] as Node.Reference }
                        }
                    }
                }
                for (i in otherNodesToMap.indices) {
                    otherNodesToMap[i] = newNodes[otherNodesToMap[i].id]!!
                }
                return PointsToGraph(this, newNodes.filterNotNull().toMutableList(), parameterNodes, variableNodes)
            }

//            fun cloneGraph(graph: PointsToGraph, otherNodesToMap: MutableList<Node>): PointsToGraph {
//                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
//                val variableNodes = mutableMapOf<IrVariable, Node.Variable>()
//                val nodes = mutableListOf<Node>()
//                graph.nodes.mapTo(nodes) { node ->
//                    node.shallowCopy().also { copy ->
//                        (copy as? Node.Parameter)?.irValueParameter?.let { parameterNodes[it] = copy }
//                        (copy as? Node.Variable)?.irVariable?.let { variableNodes[it] = copy }
//                        node.mirroredNode = copy
//                    }
//                }
//                for (node in graph.nodes) {
//                    when (val newNode = node.mirroredNode) {
//                        is Node.Object -> {
//                            (node as Node.Object).fields.entries.forEach { (field, fieldNode) ->
//                                newNode.fields[field] = fieldNode.mirroredNode as Node.FieldValue
//                            }
//                        }
//                        is Node.Reference -> {
//                            (node as Node.Reference).pointsTo.mapTo(newNode.pointsTo) { it.mirroredNode as Node.Object }
//                            node.assignedWith.mapTo(newNode.assignedWith) { it.mirroredNode as Node.Reference }
//                            node.assignedTo.mapTo(newNode.assignedTo) { it.mirroredNode as Node.Reference }
//                        }
//                    }
//                }
//                for (i in otherNodesToMap.indices) {
//                    otherNodesToMap[i] = otherNodesToMap[i].mirroredNode
//                }
//                for (node in graph.nodes)
//                    node.mirroredNode = node
//                return PointsToGraph(this, nodes.toMutableList(), parameterNodes, variableNodes)
//            }

            private fun isPrime(x: Int): Boolean {
                if (x <= 3) return true
                if (x % 2 == 0) return false
                var r = 3
                while (r * r <= x) {
                    if (x % r == 0) return false
                    r += 2
                }
                return true
            }

            private fun makePrime(p: Int): Int {
                var x = p
                while (true) {
                    if (isPrime(x)) return x
                    ++x
                }
            }

            fun mergeGraphs(graphs: List<PointsToGraph>): PointsToGraph {
                val newNodes = arrayOfNulls<Node?>(totalNodes)
                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
                val variableNodes = mutableMapOf<IrVariable, Node.Variable>()
                var assignmentEdgesCount = 0
                var pointsToEdgesCount = 0
                graphs.flatMap { it.nodes }.forEach { node ->
                    (node as? Node.Reference)?.let {
                        assignmentEdgesCount += it.assignedWith.size
                        pointsToEdgesCount += it.pointsTo.size
                    }
                    if (newNodes[node.id] == null)
                        newNodes[node.id] = node.shallowCopy().also { copy ->
                            (copy as? Node.Parameter)?.irValueParameter?.let { parameterNodes[it] = copy }
                            (copy as? Node.Variable)?.irVariable?.let { variableNodes[it] = copy }
                        }
                }

                fun addEdge(from: Int, to: Int, bagOfEdges: LongArray): Boolean {
                    val value = from.toLong() or (to.toLong() shl 32)
                    // This is 64-bit extension of a hashing method from Knuth's "The Art of Computer Programming".
                    // The magic constant is the closest prime to 2^64 * phi, where phi is the golden ratio.
                    var index = ((value.toULong() * 11400714819323198393UL) % bagOfEdges.size.toULong()).toInt()
                    while (bagOfEdges[index] != 0L && bagOfEdges[index] != value) {
                        ++index
                        if (index == bagOfEdges.size) index = 0
                    }
                    if (bagOfEdges[index] != 0L) return false
                    bagOfEdges[index] = value
                    return true
                }

                val assignmentEdges = LongArray(makePrime(5 * assignmentEdgesCount))
                val pointsToEdges = LongArray(makePrime(5 * pointsToEdgesCount))
                graphs.flatMap { it.nodes }.forEach { node ->
                    when (val newNode = newNodes[node.id]!!) {
                        is Node.Object -> {
                            (node as Node.Object).fields.forEach { (field, fieldNode) ->
                                newNode.fields.getOrPut(field) { newNodes[fieldNode.id] as Node.FieldValue }
                            }
                        }
                        is Node.Reference -> {
                            (node as Node.Reference).pointsTo.forEach {
                                if (addEdge(node.id, it.id, pointsToEdges))
                                    newNode.pointsTo.add(newNodes[it.id] as Node.Object)
                            }
                            node.assignedWith.forEach {
                                if (addEdge(node.id, it.id, assignmentEdges)) {
                                    val assignedWith = newNodes[it.id] as Node.Reference
                                    newNode.assignedWith.add(assignedWith)
                                    assignedWith.assignedTo.add(newNode)
                                }
                            }
                        }
                    }
                }
                return PointsToGraph(this, newNodes.filterNotNull().toMutableList(), parameterNodes, variableNodes)
            }
        }

        private inner class PointsToGraph(
                private val forest: PointsToGraphForest,
                val nodes: MutableList<Node>,
                val parameterNodes: Map<IrValueParameter, Node.Parameter>,
                val variableNodes: MutableMap<IrVariable, Node.Variable>
        ) {
            fun newTemporary() = Node.Variable(forest.nextNodeId(), null).also { nodes += it }
            fun getVariableNode(irVariable: IrVariable) = Node.Variable(forest.nextNodeId(), irVariable).also { nodes += it }
            fun newObject() = Node.Object(forest.nextNodeId()).also { nodes += it }

            fun Node.Object.getField(field: IrFieldSymbol) =
                    fields.getOrPut(field) { Node.FieldValue(forest.getFieldId(id, field), field).also { nodes += it } }

            fun Node.Reference.addObjectNodeIfLeaf() {
                if (pointsTo.isEmpty() && assignedWith.isEmpty()) {
                    val fictitiousObject = newObject()
                    pointsTo.add(fictitiousObject)
                }
            }

            fun addEdge(from: Node.Reference, to: Node) {
                when (to) {
                    is Node.Object -> from.pointsTo.add(to)
                    is Node.Reference -> {
                        from.assignedWith.add(to)
                        to.assignedTo.add(from)
                    }
                }
            }

            fun clone(otherNodesToMap: MutableList<Node> = mutableListOf()) = forest.cloneGraph(this, otherNodesToMap)

            // Bypasses node to its immediate pointees.
            //
            //                    *-*-*-> W2
            //                    |
            // Operation: U1 -*-> V -> O
            //                    ^
            //                    *
            //                    |
            //            U2 -*-*-*
            //
            //            *-*-*-*-*-*-*-*-*-> W1
            //            |                   ^
            //            U1 --------> O      *
            //                         ^      |
            //                         |      *
            //                         |      |
            //            U2 ----------+      *
            //            |                   |
            //            *-*-*-*-*-*-*-*-*-*-*
            //
            // Nodes naming: u -> v -> w
            fun bypass(v: Node.Reference) {
                v.addObjectNodeIfLeaf()

                for (w in v.assignedWith)
                    w.assignedTo.removeAll(v)
                for (u in v.assignedTo) {
                    u.pointsTo.addAll(v.pointsTo)
                    u.assignedWith.addAll(v.assignedWith)
                    v.assignedWith.forEach { w -> w.assignedTo.add(u) }
                }
                v.pointsTo.clear()
                v.assignedWith.clear()
                v.assignedTo.clear()
            }

            fun getObjectNodes(node: Node): List<Node.Object> = when (node) {
                is Node.Object -> listOf(node)
                is Node.Reference -> {
                    val reachable = mutableSetOf<Node.Reference>()

                    fun findReachable(node: Node.Reference) {
                        reachable.add(node)
                        node.assignedWith.forEach {
                            if (it !in reachable) findReachable(it)
                        }
                    }

                    findReachable(node)
                    reachable.flatMap {
                        it.addObjectNodeIfLeaf()
                        it.pointsTo
                    }
                }
            }

            fun logDigraph() = context.logMultiple {
                +"digraph {"

                fun Node.format(): String {
                    val name = when (this) {
                        Node.Unit -> "unit"
                        Node.Global -> "glob"
                        is Node.Parameter -> "p$id"
                        is Node.Object -> "d$id"
                        is Node.FieldValue -> "f$id"
                        is Node.Variable -> "v$id"
                        else -> error("Unknown node: $this")
                    }

                    return "$name[label=\"$this\" shape=${if (this is Node.Object) "rect" else "oval"}]"
                }

                val vertices = mutableMapOf<Node, String>()

                fun addVertex(node: Node) = vertices.getOrPut(node) { node.format().also { +"$it;" } }

                addVertex(Node.Unit)
                addVertex(Node.Global)
                nodes.forEach { addVertex(it) }

                nodes.forEach { node ->
                    val vertex = vertices[node]!!
                    when (node) {
                        is Node.Object -> {
                            node.fields.forEach { (field, fieldValue) -> +"$vertex -> ${vertices[fieldValue]!!}[label=\"${field.name}\"];" }
                        }
                        is Node.Reference -> {
                            node.pointsTo.forEach { +"$vertex -> ${vertices[it]!!};" }
                            node.assignedWith.forEach { +"$vertex -> ${vertices[it]!!};" }
                        }
                    }
                }

                +"}"
            }
        }

        private class ExpressionResult(val value: Node, val graph: PointsToGraph)

        private inner class PointsToGraphBuilder(
                val function: IrFunction,
                val forest: PointsToGraphForest,
                val escapeAnalysisResults: Map<IrFunctionSymbol, EscapeAnalysisResult>
        ) : IrElementVisitor<Node, PointsToGraph> {
            fun build(): ExpressionResult {
                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
                val pointsToGraph = PointsToGraph(forest, mutableListOf(), parameterNodes, mutableMapOf())
                function.allParameters.forEachIndexed { index, parameter ->
                    parameterNodes[parameter] = Node.Parameter(forest.nextNodeId(), index, parameter)
                }
                val returnResults = mutableListOf<ExpressionResult>()
                returnTargetsResults[function.symbol] = returnResults
                (function.body as IrBlockBody).statements.forEach { it.accept(this, pointsToGraph) }
                val functionResult = controlFlowMergePoint(pointsToGraph, function.returnType, returnResults)
                return ExpressionResult(functionResult, pointsToGraph)
            }

//            val parameterNodes = mutableMapOf<IrValueParameter, Node.Object>()
//            val variableNodes = mutableMapOf<IrVariable, Node.Reference>()

            //            fun cloneGraph(graph: PointsToGraph, alsoMapNodes: MutableList<Node>): PointsToGraph {
//                val newNodes = mutableListOf<Node>()
//                graph.nodes.mapTo(newNodes) { node ->
//                    when (node) {
//                        is Node.Object -> Node.Object(node.id)
//                        is Node.Reference -> Node.Reference(node.id)
//                    }.also { node.mirroredNode = it }
//                }
//                for (node in graph.nodes) {
//                    when (val newNode = node.mirroredNode) {
//                        is Node.Object -> {
//                            (node as Node.Object).fields.entries.forEach { (field, fieldNode) ->
//                                newNode.fields[field] = fieldNode.mirroredNode as Node.Reference
//                            }
//                        }
//                        is Node.Reference -> {
//                            (node as Node.Reference).pointsTo.mapTo(newNode.pointsTo) { it.mirroredNode as Node.Object }
//                            node.assignedWith.mapTo(newNode.assignedWith) { it.mirroredNode as Node.Reference }
//                            node.assignedTo.mapTo(newNode.assignedTo) { it.mirroredNode as Node.Reference }
//                        }
//                    }
//                }
//                for (i in alsoMapNodes.indices) {
//                    alsoMapNodes[i] = alsoMapNodes[i].mirroredNode
//                }
//                for (node in graph.nodes)
//                    node.mirroredNode = node
//                return PointsToGraph(idBuilder, newNodes)
//            }
//
            fun controlFlowMergePoint(graph: PointsToGraph, type: IrType, results: List<ExpressionResult>): Node {
                context.log { "before CFG merge" }
                graph.logDigraph()

                val mergedGraph = forest.mergeGraphs(results.map { it.graph })
                graph.nodes.clear()
                graph.nodes.addAll(mergedGraph.nodes)
                return if (type.isUnit())
                    Node.Unit
                else graph.newTemporary().also { tempNode ->
                    results.forEach { graph.addEdge(tempNode, it.value) }
                }.also {

                    context.log { "after CFG merge" }
                    graph.logDigraph()
                }
            }

            val returnTargetsResults = mutableMapOf<IrReturnTargetSymbol, MutableList<ExpressionResult>>()

            override fun visitElement(element: IrElement, data: PointsToGraph): Node = TODO(element.render())
            override fun visitExpression(expression: IrExpression, data: PointsToGraph): Node = TODO(expression.render())
            override fun visitDeclaration(declaration: IrDeclarationBase, data: PointsToGraph): Node = TODO(declaration.render())

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: PointsToGraph): Node {
                val argResult = expression.argument.accept(this, data)
                return when (expression.operator) {
                    IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.SAFE_CAST -> argResult
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> Node.Unit
                    IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> data.newTemporary()
                    else -> error("Not expected: ${expression.operator}")
                }
            }

            override fun visitConst(expression: IrConst<*>, data: PointsToGraph) = data.newObject()
            override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: PointsToGraph) = Node.Unit

            override fun visitGetValue(expression: IrGetValue, data: PointsToGraph) = when (val owner = expression.symbol.owner) {
                is IrValueParameter -> data.parameterNodes[owner] ?: error("Unknown value parameter: ${owner.render()}")
                is IrVariable -> data.variableNodes[owner] ?: error("Unknown variable: ${owner.render()}")
                else -> error("Unknown value declaration: ${owner.render()}")
            }

            override fun visitSetValue(expression: IrSetValue, data: PointsToGraph): Node {
                context.log { "before ${expression.dump()}" }
                data.logDigraph()

                val variable = expression.symbol.owner as IrVariable
                val variableNode = data.variableNodes[variable] ?: error("Unknown variable: ${variable.render()}")
                val valueNode = expression.value.accept(this, data)
                data.bypass(variableNode)
                data.addEdge(variableNode, valueNode)

                context.log { "after ${expression.dump()}" }
                data.logDigraph()

                return Node.Unit
            }

            override fun visitVariable(declaration: IrVariable, data: PointsToGraph): Node {
                val variableNode = data.getVariableNode(declaration)
                data.variableNodes[declaration] = variableNode
                declaration.initializer?.let { data.addEdge(variableNode, it.accept(this, data)) }
                return Node.Unit
            }

            override fun visitGetField(expression: IrGetField, data: PointsToGraph): Node = with(data) {
                context.log { "before ${expression.dump()}" }
                data.logDigraph()

                val receiverObjects = expression.receiver?.let { getObjectNodes(it.accept(this@PointsToGraphBuilder, data)) }
                        ?: listOf(Node.Global)
                return if (receiverObjects.size == 1)
                    receiverObjects[0].getField(expression.symbol)
                else newTemporary().also { tempNode ->
                    for (receiver in receiverObjects)
                        addEdge(tempNode, receiver.getField(expression.symbol))
                }.also {

                    context.log { "after ${expression.dump()}" }
                    data.logDigraph()
                }
            }

            override fun visitSetField(expression: IrSetField, data: PointsToGraph): Node = with(data) {
                context.log { "before ${expression.dump()}" }
                data.logDigraph()

                val receiverObjects = expression.receiver?.let { getObjectNodes(it.accept(this@PointsToGraphBuilder, data)) }
                        ?: listOf(Node.Global)
                val valueNode = expression.value.accept(this@PointsToGraphBuilder, data)
                receiverObjects.forEach {
                    val fieldNode = it.getField(expression.symbol)
                    bypass(fieldNode)
                    addEdge(fieldNode, valueNode)
                }

                context.log { "after ${expression.dump()}" }
                data.logDigraph()

                return Node.Unit
            }

            override fun visitReturn(expression: IrReturn, data: PointsToGraph): Node {
                val result = expression.value.accept(this, data)
                // TODO: Looks clumsy.
                val list = mutableListOf(result)
                val clone = data.clone(list)
                (returnTargetsResults[expression.returnTargetSymbol] ?: error("Unknown return target: ${expression.render()}"))
                        .add(ExpressionResult(list[0], clone))
                return Node.Unit // TODO: Nothing?
            }

            override fun visitContainerExpression(expression: IrContainerExpression, data: PointsToGraph): Node {
                val returnableBlockSymbol = (expression as? IrReturnableBlock)?.symbol
                returnableBlockSymbol?.let { returnTargetsResults[it] = mutableListOf() }
                expression.statements.forEachIndexed { index, statement ->
                    val result = statement.accept(this, data)
                    if (index == expression.statements.size - 1 && returnableBlockSymbol == null)
                        return result
                }
                return returnableBlockSymbol?.let {
                    controlFlowMergePoint(data, expression.type, returnTargetsResults[it]!!)
                } ?: Node.Unit
            }

            override fun visitWhen(expression: IrWhen, data: PointsToGraph): Node {
                val branchResults = mutableListOf<ExpressionResult>()
                expression.branches.forEach { branch ->
                    branch.condition.accept(this, data)
                    val branchGraph = data.clone()
                    branchResults.add(ExpressionResult(branch.result.accept(this, branchGraph), branchGraph))
                }
                val isExhaustive = expression.branches.last().isUnconditional()
                require(isExhaustive || expression.type.isUnit())
                if (!isExhaustive) {
                    // Reflecting the case when none of the clauses have been executed.
                    branchResults.add(ExpressionResult(Node.Unit, data))
                }
                return controlFlowMergePoint(data, expression.type, branchResults)
//                val mergedGraph = mergeGraphs(branchGraphs)
//                data.nodes.clear()
//                data.nodes.addAll(mergedGraph.nodes)
//
//                return if (!isExhaustive || expression.type.isUnit())
//                    Node.Unit
//                else Node.Reference(nextNodeId()).also { tempNode ->
//                    for (branchResult in branchResults)
//                        addEdge(tempNode, branchResult)
//                }
            }

            fun processCall(
                    actualCallee: IrFunction,
                    arguments: List<Node>,
                    calleeEscapeAnalysisResult: EscapeAnalysisResult,
                    graph: PointsToGraph
            ): Node {
//                context.logMultiple {
//                    +"Processing callSite"
//                    +nodeToStringWhole(call)
//                    +"Actual callee: ${callSite.actualCallee}"
//                    +"Callee escape analysis result:"
//                    +calleeEscapeAnalysisResult.toString()
//                }

/*
        return if (call is DataFlowIR.Node.NewObject) {
            (0..call.arguments.size).map {
                if (it == 0) node else call.arguments[it - 1].node
            }
        } else {
            (0..call.arguments.size).map {
                if (it < call.arguments.size) call.arguments[it].node else node
            }
        }

 */

                context.log { "before calling ${actualCallee.render()}" }
                graph.logDigraph()
                context.log { "callee EA result" }
                calleeEscapeAnalysisResult.graph.logDigraph()

                require(arguments.size == calleeEscapeAnalysisResult.parameters.size)
                val calleeGraph = calleeEscapeAnalysisResult.graph

                val returnNode = when {
                    actualCallee is IrConstructor || actualCallee.returnType.isUnit() -> Node.Unit
                    else -> graph.newTemporary()
                }
                val argumentsObjects = arguments.map { graph.getObjectNodes(it) }

                val inMirroredNodes = arrayOfNulls<Node>(calleeGraph.nodes.size) // Incoming edges.
                val outMirroredNodes = arrayOfNulls<Node>(calleeGraph.nodes.size) // Outgoing edges.

                fun reflectNode(node: Node, inMirroredNode: Node, outMirroredNode: Node = inMirroredNode) {
                    inMirroredNodes[node.id] = inMirroredNode
                    outMirroredNodes[node.id] = outMirroredNode
                }

//                if (calleeEscapeAnalysisResult.returnValue != Node.Unit)
//                    reflectNode(calleeEscapeAnalysisResult.returnValue, returnNode)
//                calleeEscapeAnalysisResult.parameters.forEachIndexed { index, parameter ->
//                    reflectNode(parameter, arguments[index])
//                    val argumentObjects = argumentsObjects[index]
//                    if (argumentObjects.size == 1) {
//                        val argumentObject = argumentObjects[0]
//                        parameter.fields.forEach { (field, fieldValue) ->
//                            reflectNode(fieldValue, with(graph) { argumentObject.getField(field) })
//                        }
//                    } else {
//                        parameter.fields.forEach { (field, fieldValue) ->
//                            val inMirroredNode = graph.newTemporary()
//                            val outMirroredNode = graph.newTemporary()
//                            reflectNode(fieldValue, inMirroredNode, outMirroredNode)
//                            argumentObjects.forEach {
//                                with(graph) {
//                                    val argumentFieldValue = it.getField(field)
//                                    addEdge(argumentFieldValue, outMirroredNode)
//                                    addEdge(inMirroredNode, argumentFieldValue)
//                                }
//                            }
//                        }
//                    }
//                }
                calleeGraph.nodes.forEach { node ->
                    when (node) {
                        calleeEscapeAnalysisResult.returnValue -> reflectNode(node, returnNode)

                        is Node.Parameter -> {
                            reflectNode(node, arguments[node.index])
                            val argumentObjects = argumentsObjects[node.index]
                            if (argumentObjects.size == 1) {
                                val argumentObject = argumentObjects[0]
                                node.fields.forEach { (field, fieldValue) ->
                                    reflectNode(fieldValue, with(graph) { argumentObject.getField(field) })
                                }
                            } else {
                                node.fields.forEach { (field, fieldValue) ->
                                    val inMirroredNode = graph.newTemporary()
                                    val outMirroredNode = graph.newTemporary()
                                    reflectNode(fieldValue, inMirroredNode, outMirroredNode)
                                    argumentObjects.forEach {
                                        with(graph) {
                                            val argumentFieldValue = it.getField(field)
                                            addEdge(argumentFieldValue, outMirroredNode)
                                            addEdge(inMirroredNode, argumentFieldValue)
                                        }
                                    }
                                }
                            }
                        }

                        is Node.Object -> {
                            val mirroredObject = graph.newObject()
                            reflectNode(node, mirroredObject)
                            node.fields.forEach { (field, fieldValue) ->
                                reflectNode(fieldValue, with(graph) { mirroredObject.getField(field) })
                            }
                        }

                        is Node.FieldValue -> Unit

                        else -> error("Unexpected node: $node")
                    }
                }

                calleeGraph.nodes.forEach { node ->
                    if (node is Node.Reference) {
                        val inMirroredNode = inMirroredNodes[node.id] as Node.Reference
                        val outMirroredNode = outMirroredNodes[node.id] as Node.Reference
                        node.pointsTo.forEach { graph.addEdge(outMirroredNode, inMirroredNodes[it.id]!!) }
                        node.assignedWith.forEach { graph.addEdge(outMirroredNode, inMirroredNodes[it.id] as Node.Reference) }
                        node.assignedTo.forEach { graph.addEdge(outMirroredNodes[it.id] as Node.Reference, inMirroredNode) }
                    }
                }

//                val calleeObjects = Array(calleeEscapeAnalysisResult.numberOfDrains) { graph.newObject() }
//
//                fun mapNode(compressedNode: CompressedPointsToGraph.Node): List<Node> {
//                    val rootNode = when (val kind = compressedNode.kind) {
//                        CompressedPointsToGraph.NodeKind.Return -> returnNode
//                        is CompressedPointsToGraph.NodeKind.Param -> arguments[kind.index]
//                        is CompressedPointsToGraph.NodeKind.Drain -> calleeObjects[kind.index]
//                    }
//                    val path = compressedNode.path
//                    var nodes = mutableListOf(rootNode)
//                    for (field in path) {
//                        val nextNodes = mutableListOf<Node>()
//                        for (node in nodes) {
//                            when (field) {
//                                returnsValueField -> nextNodes.add(node)
//                                else -> with(graph) {
//                                    getObjectNodes(node).forEach { nextNodes.add(it.getField(field)) }
//                                }
//                            }
//                        }
//                        nodes = nextNodes
//                    }
//                    return nodes
//                }
//
//                // TODO: Support escapes.
////                calleeEscapeAnalysisResult.escapes.forEach { escapingNode ->
////                    val (arg, node) = mapNode(escapingNode)
////                    if (node == null) {
////                        //context.log { "WARNING: There is no node ${nodeToString(arg!!)}" }
////                        return@forEach
////                    }
////                    escapeOrigins += node
//////                    context.log { "Node ${escapingNode.debugString(arg?.let { nodeToString(it) })} escapes" }
////                }
//
//                calleeEscapeAnalysisResult.pointsTo.edges.forEach { edge ->
//                    val fromNodes = mapNode(edge.from).map { it as Node.Reference }
//                    val toNodes = mapNode(edge.to)
//                    if (fromNodes.size == 1) {
//                        val fromNode = fromNodes[0]
//                        toNodes.forEach { graph.addEdge(fromNode, it) }
//                    } else if (toNodes.size == 1) {
//                        val toNode = toNodes[0]
//                        fromNodes.forEach { graph.addEdge(it, toNode) }
//                    } else {
//                        val temp = graph.newTemporary()
//                        fromNodes.forEach { graph.addEdge(it, temp) }
//                        toNodes.forEach { graph.addEdge(temp, it) }
//                    }
//
////                    context.logMultiple {
////                        +"Adding edge"
////                        +"    FROM ${edge.from.debugString(fromArg?.let { nodeToString(it) })}"
////                        +"    TO ${edge.to.debugString(toArg?.let { nodeToString(it) })}"
////                    }
//                }

                context.log { "after" }
                graph.logDigraph()

                return returnNode
            }

            fun processConstructorCall(
                    callee: IrConstructor,
                    thisNode: Node,
                    callSite: IrFunctionAccessExpression,
                    graph: PointsToGraph
            ) {
                val arguments = listOf(thisNode) + callSite.getArgumentsWithIr().map { it.second.accept(this, graph) }
                processCall(callee, arguments, escapeAnalysisResults[callee.symbol]!!, graph)
            }

            override fun visitConstructorCall(expression: IrConstructorCall, data: PointsToGraph): Node {
                val thisObject = data.newObject()
                processConstructorCall(expression.symbol.owner, thisObject, expression, data)
                return thisObject
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: PointsToGraph): Node {
                val constructor = expression.symbol.owner
                val thisReceiver = (function as IrConstructor).constructedClass.thisReceiver!!
                val irThis = IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, thisReceiver.type, thisReceiver.symbol)
                processConstructorCall(constructor, irThis.accept(this, data), expression, data)
                return Node.Unit
            }

            val createUninitializedInstanceSymbol = context.ir.symbols.createUninitializedInstance
            val initInstanceSymbol = context.ir.symbols.initInstance

            override fun visitCall(expression: IrCall, data: PointsToGraph): Node {
                if (!expression.isVirtualCall) {
                    // TODO: What about staticInitializer, executeImpl, getContinuation?
                    return when (expression.symbol) {
                        createUninitializedInstanceSymbol -> data.newObject()
                        initInstanceSymbol -> {
                            val irThis = expression.getValueArgument(0)!!
                            val thisNode = irThis.accept(this, data)
                            val irInitializer = expression.getValueArgument(1) as IrConstructorCall
                            processConstructorCall(irInitializer.symbol.owner, thisNode, irInitializer, data)
                            Node.Unit
                        }
                        else -> {
                            val actualCallee = expression.actualCallee
                            val arguments = expression.getArgumentsWithIr().map { it.second.accept(this, data) }
                            processCall(actualCallee, arguments, escapeAnalysisResults[actualCallee.symbol]!!, data)
                        }
                    }
                }
                TODO("Not implemented")
            }
        }

        private fun intraproceduralAnalysis(callGraphNode: CallGraphNode,
                                            escapeAnalysisResults: MutableMap<IrFunctionSymbol, EscapeAnalysisResult>) {
            val function = callGraphNode.symbol.irFunction ?: return
            if (function.body == null) return

            val producerInvocations = mutableMapOf<IrExpression, IrCall>()
            val jobInvocations = mutableMapOf<IrCall, IrCall>()
            val virtualCallSites = mutableMapOf<IrCall, MutableList<CallGraphNode.CallSite>>()
            for (callSite in callGraphNode.callSites) {
                val call = callSite.call
                val irCall = call.irCallSite as? IrCall ?: continue
                if (irCall.origin == STATEMENT_ORIGIN_PRODUCER_INVOCATION)
                    producerInvocations[irCall.dispatchReceiver!!] = irCall
                else if (irCall.origin == STATEMENT_ORIGIN_JOB_INVOCATION)
                    jobInvocations[irCall.getValueArgument(0) as IrCall] = irCall
                if (call !is DataFlowIR.Node.VirtualCall) continue
                virtualCallSites.getOrPut(irCall) { mutableListOf() }.add(callSite)
            }

            val forest = PointsToGraphForest()
            val functionResult = PointsToGraphBuilder(function, forest, escapeAnalysisResults).build()

            // TODO: compute lifetimes.

            /*
             * Now, do the following:
             * 1. Add an object node to each leaf variable node.
             * 2. Remove all variable nodes (but no sooner than bypassing them) except the field nodes.
             * 3. Find all object nodes, reachable from the parameters (including the return parameter).
             * 4. Observation 1: if a field node have no incoming (except the edge from its parent)
             *                   or outgoing edges, it can be omitted.
             *    Observation 2: if an object node has no field nodes and has only one incoming edge,
             *                   it also can be omitted.
             *    So, we can eliminate nodes until none of the above observations is true.
             */

            // Looks like this is not needed - it will be done during bypassing phase.
//            with(functionResult.graph) {
//                nodes.filterIsInstance<Node.Reference>().forEach { it.addObjectNodeIfLeaf() }
//            }

//            val returnObject = (functionResult.value as? Node.Object)
//                    ?: with(functionResult.graph) {
//                        newObject().also { addEdge(it.getField(returnsValueField), functionResult.value) }
//                    }

            val reachable = mutableSetOf<Node>()
            findReachable(functionResult.value, reachable)
            functionResult.graph.parameterNodes.values.forEach {
                if (it !in reachable) findReachable(it, reachable)
            }

            val nodes = mutableListOf<Node>()
            reachable.forEach { node ->
                if (node is Node.Variable && node != functionResult.value)
                    functionResult.graph.bypass(node)
                else
                    nodes.add(node)
            }

            nodes.forEachIndexed { index, node -> node.id = index }

            // Remove multi-edges.
            val graph = forest.mergeGraphs(listOf(PointsToGraph(forest, nodes, functionResult.graph.parameterNodes, mutableMapOf())))

            // TODO: Remove redundant fields and objects.

            context.log { "EA result for ${function.render()}" }
            graph.logDigraph()

            escapeAnalysisResults[function.symbol] = EscapeAnalysisResult(graph, functionResult.value,
                    function.allParameters.map { graph.parameterNodes[it]!! })
        }

        private fun findReachable(node: Node, visited: MutableSet<Node>) {
            visited.add(node)
            when (node) {
                is Node.Object -> {
                    node.fields.values.forEach {
                        if (it !in visited) findReachable(it, visited)
                    }
                }
                is Node.Reference -> {
                    node.pointsTo.forEach {
                        if (it !in visited) findReachable(it, visited)
                    }
                    node.assignedWith.forEach {
                        if (it !in visited) findReachable(it, visited)
                    }
                }
            }
        }


    }

    @Suppress("UNUSED_PARAMETER")
    fun computeLifetimes(context: Context, callGraph: CallGraph, lifetimes: MutableMap<IrElement, Lifetime>) {
        try {
            InterproceduralAnalysis(context, callGraph).analyze()
        } catch (t: Throwable) {
            val extraUserInfo =
                    """
                        Please try to disable escape analysis and rerun the build. To do it add the following snippet to the gradle script:

                            kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
                                binaries.all {
                                    freeCompilerArgs += "-Xdisable-phases=EscapeAnalysis"
                                }
                            }

                        In case of using command line compiler add this option: "-Xdisable-phases=EscapeAnalysis".
                        Also, consider filing an issue with full Gradle log here: https://kotl.in/issue
                        """.trimIndent()
            context.reportCompilationError("Escape analysis failure:\n$extraUserInfo\n\n${t.message}\n${t.stackTraceToString()}")
        }
    }
}
