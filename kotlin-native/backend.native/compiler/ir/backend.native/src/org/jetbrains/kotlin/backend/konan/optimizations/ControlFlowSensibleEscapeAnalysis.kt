/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DirectedGraphCondensationBuilder
import org.jetbrains.kotlin.backend.konan.DirectedGraphMultiNode
import org.jetbrains.kotlin.backend.konan.ir.actualCallee
import org.jetbrains.kotlin.backend.konan.ir.isUnconditional
import org.jetbrains.kotlin.backend.konan.ir.isVirtualCall
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.logMultiple
import org.jetbrains.kotlin.backend.konan.lower.erasedUpperBound
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irDoWhile
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.util.*
import kotlin.collections.ArrayList

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

private fun <T> MutableList<T?>.ensureSize(newSize: Int) {
    for (i in size until newSize) add(null)
}

private fun <T> MutableList<T>.trimSize(newSize: Int) {
    for (i in newSize until size) removeAt(size - 1)
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

        private class EscapeAnalysisResult(val graph: PointsToGraph, val returnValue: Node) {
            companion object {
                fun optimistic(returnType: IrType, parameters: List<IrValueParameter>): EscapeAnalysisResult {
                    val pointsToGraph = PointsToGraph(PointsToGraphForest())
                    parameters.forEachIndexed { index, parameter -> pointsToGraph.addParameter(parameter, index) }
                    val returnValue = with(pointsToGraph) {
                        if (returnType.isUnit()) Node.Unit else newTempVariable()
                    }
                    return EscapeAnalysisResult(pointsToGraph, returnValue)
                }
            }
        }

        // TODO: Add a special Null node.
        private sealed class Node(var id: Int) {
//            @Suppress("LeakingThis")
//            var mirroredNode = this

            abstract fun shallowCopy(): Node

            // TODO: Do we need to distinguish fictitious objects and materialized ones?
            open class Object(id: Int, val loop: IrLoop?, val label: String? = null) : Node(id) {
                val fields = mutableMapOf<IrFieldSymbol, FieldValue>()

                val isFictitious get() = label == null

                override fun shallowCopy() = Object(id, loop, label)
                override fun toString() = "${label ?: "D"}$id"
            }

            class Parameter(id: Int, val index: Int, val irValueParameter: IrValueParameter) : Object(id, null) {
                override fun shallowCopy() = Parameter(id, index, irValueParameter)
                override fun toString() = "<P:${irValueParameter.name}[$index]>$id"
            }

            sealed class Reference(id: Int) : Node(id) {
                abstract fun addEdge(to: Node)
            }

            sealed class Variable(id: Int) : Reference(id) {
                var assignedWith: Node? = null
                val assignedTo = mutableListOf<Reference>()

                override fun addEdge(to: Node) {
                    require(assignedWith == null) {
                        "A bypassing operation should've been applied before reassigning a variable $this"
                    }
                    assignedWith = to
                    (to as? Variable)?.assignedTo?.add(this)
                }
            }

            class Phi(id: Int) : Reference(id) {
                val pointsTo = mutableListOf<Node>()

                override fun shallowCopy() = Phi(id)
                override fun toString() = "φ$id"

                override fun addEdge(to: Node) {
                    pointsTo.add(to)
                    (to as? Variable)?.assignedTo?.add(this)
                }
            }

            class FieldValue(id: Int, val ownerId: Int, val field: IrFieldSymbol) : Variable(id) {
                // TODO: Looks like this should be computed AFTER the loop.
                val pinnedPointsTo = mutableListOf<Object>() // Objects that have been created inside a loop.

                override fun shallowCopy() = FieldValue(id, ownerId, field)
                override fun toString() = "F$id"
            }

            class VariableValue(id: Int, val irVariable: IrVariable? = null) : Variable(id) {
                override fun shallowCopy() = VariableValue(id, irVariable)
                override fun toString() = irVariable?.let { "<V:${it.name}>$id" } ?: "T$id"
            }

            object Unit : Object(UNIT_ID, null, "<U>") {
                override fun shallowCopy() = this
            }

            companion object {
                const val UNIT_ID = 0
                const val GLOBAL_ID = 1
                const val LOWEST_NODE_ID = 2
            }
        }

        private class PointsToGraphForest(startNodeId: Int = Node.LOWEST_NODE_ID) {
            private var currentNodeId = startNodeId
            fun nextNodeId() = currentNodeId++
            val totalNodes get() = currentNodeId

            // TODO: Optimize.
            private val ids = mutableMapOf<Pair<Int, Any>, Int>()
            fun getAssociatedId(nodeId: Int, obj: Any) = ids.getOrPut(Pair(nodeId, obj)) { nextNodeId() }

//            private val fieldIds = mutableMapOf<Pair<Int, IrFieldSymbol>, Int>()
//            fun getFieldId(objectId: Int, field: IrFieldSymbol) = fieldIds.getOrPut(Pair(objectId, field)) { nextNodeId() }

//            private val irElementIds = mutableMapOf<IrElement, Int>()
//            fun getIrElementId(element: IrElement) = irElementIds.getOrPut(element) { nextNodeId() }

            fun cloneGraph(graph: PointsToGraph, otherNodesToMap: MutableList<Node>): PointsToGraph {
                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
                val variableNodes = mutableMapOf<IrVariable, Node.VariableValue>()
                val newNodes = ArrayList<Node?>(graph.nodes.size)
                graph.nodes.mapTo(newNodes) { node ->
                    node?.shallowCopy()?.also { copy ->
                        (copy as? Node.Parameter)?.irValueParameter?.let { parameterNodes[it] = copy }
                        (copy as? Node.VariableValue)?.irVariable?.let { variableNodes[it] = copy }
                    }
                }
                for (node in graph.nodes) {
                    if (node == null) continue
                    when (val newNode = newNodes[node.id]!!) {
                        is Node.Object -> {
                            (node as Node.Object).fields.entries.forEach { (field, fieldNode) ->
                                newNode.fields[field] = newNodes[fieldNode.id] as Node.FieldValue
                            }
                        }
                        is Node.Variable -> {
                            newNode.assignedWith = (node as Node.Variable).assignedWith?.let { newNodes[it.id]!! }
                            node.assignedTo.mapTo(newNode.assignedTo) { newNodes[it.id] as Node.Reference }
                            if (newNode is Node.FieldValue) {
                                (node as Node.FieldValue).pinnedPointsTo.mapTo(newNode.pinnedPointsTo) { newNodes[it.id] as Node.Object }
                            }
                        }
                        is Node.Phi -> {
                            (node as Node.Phi).pointsTo.mapTo(newNode.pointsTo) { newNodes[it.id]!! }
                        }
                    }
                }
                for (i in otherNodesToMap.indices)
                    otherNodesToMap[i] = newNodes[otherNodesToMap[i].id]!!
                return PointsToGraph(this, newNodes, parameterNodes, variableNodes)
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

            fun mergeGraphs(graphs: List<PointsToGraph>, element: IrElement?, otherNodesToMap: MutableList<Node>): PointsToGraph {
                val newNodes = ArrayList<Node?>(totalNodes).also { it.ensureSize(totalNodes) }
                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
                val variableNodes = mutableMapOf<IrVariable, Node.VariableValue>()
                var edgesCount = 0
                graphs.flatMap { it.nodes }.forEach { node ->
                    if (node == null) return@forEach
                    (node as? Node.Phi)?.let { edgesCount += it.pointsTo.size }
                    (node as? Node.Variable)?.let { if (it.assignedWith != null) ++edgesCount }
                    (node as? Node.FieldValue)?.let { edgesCount += it.pinnedPointsTo.size }
                    if (newNodes[node.id] == null)
                        newNodes[node.id] = node.shallowCopy().also { copy ->
                            (copy as? Node.Parameter)?.irValueParameter?.let { parameterNodes[it] = copy }
                            (copy as? Node.VariableValue)?.irVariable?.let { variableNodes[it] = copy }
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

                val edges = LongArray(makePrime(5 * edgesCount))
                graphs.flatMap { it.nodes }.forEach { node ->
                    if (node == null) return@forEach
                    when (val newNode = newNodes[node.id]!!) {
                        is Node.Object -> {
                            (node as Node.Object).fields.forEach { (field, fieldNode) ->
                                newNode.fields.getOrPut(field) { newNodes[fieldNode.id] as Node.FieldValue }
                            }
                        }
                        is Node.Phi -> {
                            (node as Node.Phi).pointsTo.forEach {
                                if (addEdge(node.id, it.id, edges))
                                    newNode.pointsTo += newNodes[it.id]!!
                            }
                        }
                        is Node.Variable -> {
                            if (newNode is Node.FieldValue) {
                                (node as Node.FieldValue).pinnedPointsTo.forEach {
                                    if (addEdge(node.id, it.id, edges))
                                        newNode.pinnedPointsTo += newNodes[it.id] as Node.Object
                                }
                            }

                            val prevAssignedWith = newNode.assignedWith
                            val assignedWith = (node as Node.Variable).assignedWith ?: return@forEach
                            if (prevAssignedWith == null)
                                newNode.assignedWith = newNodes[assignedWith.id]!!
                            else if (assignedWith.id != prevAssignedWith.id) {
                                val phiNode = if (prevAssignedWith.id >= newNodes.size)
                                    prevAssignedWith as Node.Phi
                                else Node.Phi(element?.let { getAssociatedId(newNode.id, it) } ?: nextNodeId()).also {
                                    newNodes.ensureSize(it.id + 1)
                                    newNodes[it.id] = it
                                    newNode.assignedWith = it
                                    addEdge(it.id, prevAssignedWith.id, edges)
                                    it.pointsTo += prevAssignedWith
                                }
                                if (addEdge(phiNode.id, assignedWith.id, edges)) {
                                    phiNode.pointsTo += newNodes[assignedWith.id]!!
                                }
                            }
                        }
                    }
                }

                for (i in otherNodesToMap.indices)
                    otherNodesToMap[i] = newNodes[otherNodesToMap[i].id]!!
                for (node in newNodes) {
                    ((node as? Node.Variable)?.assignedWith as? Node.Variable)?.assignedTo?.add(node)
                    (node as? Node.Phi)?.pointsTo?.forEach { (it as? Node.Variable)?.assignedTo?.add(node) }
                }
                return PointsToGraph(this, newNodes, parameterNodes, variableNodes)
            }

            fun graphsAreEqual(first: PointsToGraph, second: PointsToGraph): Boolean {
                if (first.nodes.size != second.nodes.size) return false
                val firstPointsTo = BitSet(first.nodes.size)
                val secondPointsTo = BitSet(first.nodes.size)
                for (id in 0 until first.nodes.size) {
                    val firstNode = first.nodes[id]
                    val secondNode = second.nodes[id]
                    when (firstNode) {
                        null -> if (secondNode != null) return false
                        is Node.Object -> {
                            if (secondNode !is Node.Object) return false
                            if (firstNode.fields.size != secondNode.fields.size) return false
                            firstNode.fields.forEach { (field, fieldValue) ->
                                if (fieldValue.id != secondNode.fields[field]?.id) return false
                            }
                        }
                        is Node.VariableValue -> {
                            if (secondNode !is Node.VariableValue) return false
                            if ((firstNode.assignedWith?.id ?: -1) != (secondNode.assignedWith?.id ?: -1)) return false
                        }
                        is Node.Phi -> {
                            if (secondNode !is Node.Phi) return false
                            firstNode.pointsTo.forEach { firstPointsTo.set(it.id) }
                            secondNode.pointsTo.forEach { secondPointsTo.set(it.id) }
                            firstNode.pointsTo.forEach { if (!secondPointsTo.get(it.id)) return false }
                            secondNode.pointsTo.forEach { if (!firstPointsTo.get(it.id)) return false }
                            firstNode.pointsTo.forEach { firstPointsTo.clear(it.id) }
                            secondNode.pointsTo.forEach { secondPointsTo.clear(it.id) }
                        }
                        is Node.FieldValue -> {
                            if (secondNode !is Node.FieldValue) return false
                            if ((firstNode.assignedWith?.id ?: -1) != (secondNode.assignedWith?.id ?: -1)) return false
                            firstNode.pinnedPointsTo.forEach { firstPointsTo.set(it.id) }
                            secondNode.pinnedPointsTo.forEach { secondPointsTo.set(it.id) }
                            firstNode.pinnedPointsTo.forEach { if (!secondPointsTo.get(it.id)) return false }
                            secondNode.pinnedPointsTo.forEach { if (!firstPointsTo.get(it.id)) return false }
                            firstNode.pinnedPointsTo.forEach { firstPointsTo.clear(it.id) }
                            secondNode.pinnedPointsTo.forEach { secondPointsTo.clear(it.id) }
                        }
                    }
                }

                return true
            }
        }

        private interface NodeFactory {
            fun newTempVariable(): Node.VariableValue
            fun newPhiNode(): Node.Phi
            fun newObject(label: String? = null): Node.Object
        }

        private class PointsToGraph(
                val forest: PointsToGraphForest,
                val nodes: MutableList<Node?>,
                val parameterNodes: MutableMap<IrValueParameter, Node.Parameter>,
                val variableNodes: MutableMap<IrVariable, Node.VariableValue>
        ) : NodeFactory {
            constructor(forest: PointsToGraphForest)
                    : this(forest, mutableListOf(Node.Unit, Node.Object(Node.GLOBAL_ID, null, "<G>")), mutableMapOf(), mutableMapOf())

            private inline fun <reified T : Node> getOrPutNodeAt(id: Int, nodeBuilder: (Int) -> T): T {
                nodes.ensureSize(id + 1)
                nodes[id]?.let { return it as T }
                return nodeBuilder(id).also { nodes[id] = it }
            }

            private inline fun <reified T : Node> putNewNodeAt(id: Int, nodeBuilder: (Int) -> T): T {
                nodes.ensureSize(id + 1)
                val node = nodeBuilder(id)
                require(nodes[id] == null) { "Duplicate node at $id: ${nodes[id]} and $node" }
                nodes[id] = node
                return node
            }

            inner class NodeContext(var currentNodeId: Int, val loop: IrLoop) : NodeFactory {
                private fun getNodeId(): Int {
                    require(currentNodeId != -1) { "Context can only create one node" }
                    return currentNodeId.also { currentNodeId = -1 }
                }

                override fun newTempVariable() = getOrPutNodeAt(getNodeId()) { Node.VariableValue(it) }
                override fun newPhiNode() = getOrPutNodeAt(getNodeId()) { Node.Phi(it) }
                override fun newObject(label: String?) = getOrPutNodeAt(getNodeId()) { Node.Object(it, loop, label) }
            }

            val globalNode get() = nodes[Node.GLOBAL_ID] as Node.Object

            fun copyFrom(other: PointsToGraph) {
                nodes.clear()
                nodes.addAll(other.nodes)
                other.parameterNodes.forEach { (parameter, parameterNode) -> parameterNodes[parameter] = parameterNode }
                other.variableNodes.forEach { (variable, variableNode) -> variableNodes[variable] = variableNode }
            }

            fun addParameter(parameter: IrValueParameter, index: Int) = putNewNodeAt(forest.nextNodeId()) { id ->
                Node.Parameter(id, index, parameter).also { parameterNodes[parameter] = it }
            }

            fun getOrAddVariable(irVariable: IrVariable) = variableNodes.getOrPut(irVariable) {
                putNewNodeAt(forest.nextNodeId()) { Node.VariableValue(it, irVariable) }
            }

            override fun newTempVariable() = getOrPutNodeAt(forest.nextNodeId()) { Node.VariableValue(it) }
            override fun newPhiNode() = getOrPutNodeAt(forest.nextNodeId()) { Node.Phi(it) }
            override fun newObject(label: String?) = getOrPutNodeAt(forest.nextNodeId()) { Node.Object(it, null, label) }

            fun at(loop: IrLoop?, element: IrElement?, anchorNodeId: Int = Node.UNIT_ID): NodeFactory =
                    loop?.let { NodeContext(forest.getAssociatedId(anchorNodeId, element!!), it) } ?: this

            fun Node.Object.getField(field: IrFieldSymbol) = fields.getOrPut(field) {
                putNewNodeAt(forest.getAssociatedId(id, field)) { Node.FieldValue(it, id, field) }
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
            fun bypass(v: Node.Variable, loop: IrLoop?, element: IrElement?, anchorNodeId: Int = Node.UNIT_ID) {
                require(v.assignedTo.isNotEmpty())
                val w = v.assignedWith ?: at(loop, element, anchorNodeId).newObject()
                (w as? Node.Variable)?.assignedTo?.run {
                    removeAll(v)
                    addAll(v.assignedTo)
                }
                for (u in v.assignedTo) {
                    when (u) {
                        is Node.Variable ->
                            u.assignedWith = w
                        is Node.Phi -> {
                            u.pointsTo.removeAll(v)
                            u.pointsTo.add(w)
                        }
                    }
                }
                v.assignedWith = null
                v.assignedTo.clear()
            }

            fun getObjectNodes(node: Node, createFictitiousObjects: Boolean, loop: IrLoop?, element: IrElement?,
                               anchorNodeId: Int = Node.UNIT_ID): List<Node.Object> = when (node) {
                is Node.Object -> listOf(node)
                is Node.Reference -> {
                    val reachable = mutableSetOf<Node.Reference>()

                    fun findReachable(node: Node.Reference) {
                        reachable.add(node)
                        when (node) {
                            is Node.Variable -> {
                                (node.assignedWith as? Node.Reference)?.let {
                                    if (it !in reachable) findReachable(it)
                                }
                            }
                            is Node.Phi ->
                                for (pointee in node.pointsTo)
                                    (pointee as? Node.Reference)?.let {
                                        if (it !in reachable) findReachable(it)
                                    }
                        }
                    }

                    findReachable(node)
                    buildList {
                        for (reachableNode in reachable) {
                            when (reachableNode) {
                                is Node.Phi -> {
                                    for (pointee in reachableNode.pointsTo)
                                        (pointee as? Node.Object)?.let { add(it) }
                                }
                                is Node.Variable -> {
                                    (node as? Node.FieldValue)?.pinnedPointsTo?.let { addAll(it) }

                                    val assignedWith = reachableNode.assignedWith
                                    if (assignedWith != null)
                                        (assignedWith as? Node.Object)?.let { add(it) }
                                    else if (createFictitiousObjects) {
                                        val fictitiousObject = at(loop, element, anchorNodeId).newObject()
                                        reachableNode.assignedWith = fictitiousObject
                                        add(fictitiousObject)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            fun log(context: Context) = context.logMultiple {
                fun Node.format() = "$id ${toString()} ${System.identityHashCode(this)}"

                fun Node.formatIncomingEdge() = when (this) {
                    is Node.Object -> "--->"
                    is Node.Reference -> "-*->"
                }

                +"parameters:"
                parameterNodes.values.forEach { +"    ${it.format()}" }
                +"variables:"
                variableNodes.values.forEach { +"    ${it.format()}" }
                +"nodes:"
                nodes.forEach { node -> node?.let { +"    ${it.format()}" } }
                +"edges:"
                nodes.forEach { node ->
                    when (node) {
                        null -> return@forEach
                        is Node.Object -> {
                            node.fields.forEach { (field, fieldValue) ->
                                +"    ${node.format()} ---> ${fieldValue.format()}[field=${field.name}]"
                            }
                        }
                        is Node.Variable -> {
                            node.assignedWith?.let { +"    ${node.format()} ${it.formatIncomingEdge()} ${it.format()}" }
                            node.assignedTo.forEach { +"    ${node.format()} <-*- ${it.format()}" }
                        }
                        is Node.Phi -> {
                            node.pointsTo.forEach { +"    ${node.format()} ${it.formatIncomingEdge()} ${it.format()}" }
                        }
                    }
                }
            }

            fun logDigraph(context: Context, vararg markedNodes: Node) = logDigraph(context, markedNodes.asList())

            fun logDigraph(context: Context, markedNodes: List<Node>) = context.logMultiple {
//                log(context)

                +"digraph {"
                +"rankdir=\"LR\";"

                fun Node.format(): String {
                    val name = when (this) {
                        Node.Unit -> "unit"
                        globalNode -> "glob"
                        is Node.Parameter -> "param$id"
                        is Node.Object -> "obj$id"
                        is Node.FieldValue -> "field$id"
                        is Node.Variable -> "var$id"
                        is Node.Phi -> "phi$id"
                    }

                    val label = "label=\"$this\""
                    val shape = " shape=${if (this is Node.Object) "rect" else "oval"}"
                    val colors = buildString {
                        if ((this@format as? Node.Object)?.loop != null)
                            append(" penwidth=3.0 color=deepskyblue")
                        if (this@format in markedNodes)
                            append(" fillcolor=yellow style=filled")
                    }
                    +"$name[$label$shape$colors]"
                    return name
                }

                val notNullNodes = nodes.filterNotNull()
                val vertices = notNullNodes.associateWith { it.format() }

                notNullNodes.forEach { node ->
                    val vertex = vertices[node]!!
                    when (node) {
                        is Node.Object ->
                            node.fields.forEach { (field, fieldValue) -> +"$vertex -> ${vertices[fieldValue] ?: error("No node ${fieldValue.id} in graph")}[label=\"${field.name}\"];" }
                        is Node.VariableValue ->
                            node.assignedWith?.let { +"$vertex -> ${vertices[it] ?: error("No node ${it.id} in graph")};" }
                        is Node.FieldValue -> {
                            node.assignedWith?.let { +"$vertex -> ${vertices[it] ?: error("No node ${it.id} in graph")};" }
                            node.pinnedPointsTo.forEach { +"$vertex -> ${vertices[it] ?: error("No node ${it.id} in graph")}[penwidth=2.0 color=deepskyblue];" }
                        }
                        is Node.Phi ->
                            node.pointsTo.forEach { +"$vertex -> ${vertices[it] ?: error("No node ${it.id} in graph")};" }
                    }
                }

                +"}"
            }
        }

        private class ExpressionResult(val value: Node, val graph: PointsToGraph)

        private data class BuilderState(val graph: PointsToGraph, val loop: IrLoop?)

        private inner class PointsToGraphBuilder(
                val function: IrFunction,
                val forest: PointsToGraphForest,
                val escapeAnalysisResults: Map<IrFunctionSymbol, EscapeAnalysisResult>
        ) : IrElementVisitor<Node, BuilderState> {
            fun build(): ExpressionResult {
                val pointsToGraph = PointsToGraph(forest)
                function.allParameters.forEachIndexed { index, parameter -> pointsToGraph.addParameter(parameter, index) }
                val returnResults = mutableListOf<ExpressionResult>()
                returnTargetsResults[function.symbol] = returnResults
                (function.body as IrBlockBody).statements.forEach { it.accept(this, BuilderState(pointsToGraph, null)) }
                val functionResult = controlFlowMergePoint(pointsToGraph, null, null, function.returnType, returnResults)
                return ExpressionResult(functionResult, pointsToGraph)
            }

            fun controlFlowMergePoint(graph: PointsToGraph, loop: IrLoop?, element: IrElement?, type: IrType, results: List<ExpressionResult>): Node {
                return if (results.size == 1) {
                    graph.copyFrom(results[0].graph)
                    if (type.isUnit()) Node.Unit else results[0].value
                } else {
                    context.log { "before CFG merge" }
                    results.forEachIndexed { index, result ->
                        context.log { "#$index:" }
                        result.graph.logDigraph(context, result.value)
                    }

                    val resultNodes = results.map { it.value }.toMutableList()
                    val mergedGraph = forest.mergeGraphs(results.map { it.graph }, element, resultNodes)
                    graph.copyFrom(mergedGraph)
                    if (type.isUnit())
                        Node.Unit
                    else graph.at(loop, element).newPhiNode().also { phiNode ->
                        resultNodes.forEach { phiNode.addEdge(it) }
                    }
                }.also {

                    context.log { "after CFG merge" }
                    graph.logDigraph(context, it)
                }
            }

            val returnTargetsResults = mutableMapOf<IrReturnTargetSymbol, MutableList<ExpressionResult>>()

            override fun visitElement(element: IrElement, data: BuilderState): Node = TODO(element.render())
            override fun visitExpression(expression: IrExpression, data: BuilderState): Node = TODO(expression.render())
            override fun visitDeclaration(declaration: IrDeclarationBase, data: BuilderState): Node = TODO(declaration.render())

            fun BuilderState.constObjectNode(expression: IrExpression) =
                    graph.at(loop, expression).newObject("<C:${expression.type.erasedUpperBound.name}>")

            override fun visitConst(expression: IrConst<*>, data: BuilderState) =
                    data.constObjectNode(expression)

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BuilderState): Node {
                val argResult = expression.argument.accept(this, data)
                return when (expression.operator) {
                    IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.SAFE_CAST -> argResult
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> Node.Unit
                    IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> data.constObjectNode(expression)
                    else -> error("Not expected: ${expression.operator}")
                }
            }

            override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: BuilderState) = Node.Unit

            override fun visitGetValue(expression: IrGetValue, data: BuilderState) = when (val owner = expression.symbol.owner) {
                is IrValueParameter -> data.graph.parameterNodes[owner] ?: error("Unknown value parameter: ${owner.render()}")
                is IrVariable -> data.graph.variableNodes[owner] ?: error("Unknown variable: ${owner.render()}")
                else -> error("Unknown value declaration: ${owner.render()}")
            }

            fun PointsToGraph.setValue(variableNode: Node.Variable, valueNode: Node, loop: IrLoop?, element: IrElement) {
                if (variableNode.assignedTo.isNotEmpty())
                    bypass(variableNode, loop, element)
                else
                    variableNode.assignedWith = null
                variableNode.addEdge(valueNode)
            }

            override fun visitSetValue(expression: IrSetValue, data: BuilderState): Node = with(data.graph) {
                context.log { "before ${expression.dump()}" }
                logDigraph(context)

                val valueNode = expression.value.accept(this@PointsToGraphBuilder, data)
                context.log { "after evaluating value" }
                logDigraph(context, valueNode)

                val variable = expression.symbol.owner as IrVariable
                val variableNode = variableNodes[variable] ?: error("Unknown variable: ${variable.render()}")
                setValue(variableNode, valueNode, data.loop, expression)
                context.log { "after ${expression.dump()}" }
                logDigraph(context, variableNode)

                Node.Unit
            }

            override fun visitVariable(declaration: IrVariable, data: BuilderState): Node = with(data.graph) {
                context.log { "before ${declaration.dump()}" }
                logDigraph(context)

                require(data.loop != null || variableNodes[declaration] == null) {
                    "Duplicate variable declaration: ${declaration.render()}"
                }

                val valueNode = declaration.initializer?.accept(this@PointsToGraphBuilder, data)
                valueNode?.let {
                    context.log { "after evaluating initializer" }
                    logDigraph(context, it)
                }

                val variableNode = getOrAddVariable(declaration)
                valueNode?.let { setValue(variableNode, it, data.loop, declaration) }
                context.log { "after ${declaration.dump()}" }
                logDigraph(context, variableNode)

                Node.Unit
            }

            override fun visitGetField(expression: IrGetField, data: BuilderState): Node = with(data.graph) {
                context.log { "before ${expression.dump()}" }
                logDigraph(context)

                val receiverNode = expression.receiver?.accept(this@PointsToGraphBuilder, data) ?: globalNode
                context.log { "after evaluating receiver" }
                logDigraph(context, receiverNode)

                val receiverObjects = getObjectNodes(receiverNode, true, data.loop, expression.receiver)
                context.log { "after getting receiver's objects" }
                logDigraph(context, receiverObjects)

                return (if (receiverObjects.size == 1)
                    receiverObjects[0].getField(expression.symbol)
                else at(data.loop, expression).newPhiNode().also { phiNode ->
                    for (receiver in receiverObjects)
                        phiNode.addEdge(receiver.getField(expression.symbol))
                }).also {
                    context.log { "after ${expression.dump()}" }
                    logDigraph(context, it)
                }
            }

            override fun visitSetField(expression: IrSetField, data: BuilderState): Node = with(data.graph) {
                context.log { "before ${expression.dump()}" }
                logDigraph(context)

                val receiverNode = expression.receiver?.accept(this@PointsToGraphBuilder, data) ?: globalNode
                context.log { "after evaluating receiver" }
                logDigraph(context, receiverNode)

                val receiverObjects = getObjectNodes(receiverNode, true, data.loop, expression.receiver)
                context.log { "after getting receiver's objects" }
                logDigraph(context, receiverObjects)

                val valueNode = expression.value.accept(this@PointsToGraphBuilder, data)
                context.log { "after evaluating value" }
                logDigraph(context, valueNode)

                receiverObjects.forEach { receiverObject ->
                    val fieldNode = receiverObject.getField(expression.symbol)
                    setValue(fieldNode, valueNode, data.loop, expression)
                }
                context.log { "after ${expression.dump()}" }
                logDigraph(context)

                return Node.Unit
            }

            override fun visitReturn(expression: IrReturn, data: BuilderState): Node {
                val result = expression.value.accept(this, data)
                // TODO: Looks clumsy.
                val list = mutableListOf(result)
                val clone = data.graph.clone(list)
                (returnTargetsResults[expression.returnTargetSymbol] ?: error("Unknown return target: ${expression.render()}"))
                        .add(ExpressionResult(list[0], clone))
                // TODO: Return null (=Nothing) and clear the graph.
                return Node.Unit
            }

            override fun visitContainerExpression(expression: IrContainerExpression, data: BuilderState): Node {
                val returnableBlockSymbol = (expression as? IrReturnableBlock)?.symbol
                returnableBlockSymbol?.let { returnTargetsResults[it] = mutableListOf() }
                expression.statements.forEachIndexed { index, statement ->
                    val result = statement.accept(this, data)
                    if (index == expression.statements.size - 1 && returnableBlockSymbol == null)
                        return result
                }
                return returnableBlockSymbol?.let {
                    controlFlowMergePoint(data.graph, data.loop, expression, expression.type, returnTargetsResults[it]!!)
                } ?: Node.Unit
            }

            override fun visitWhen(expression: IrWhen, data: BuilderState): Node {
                val branchResults = mutableListOf<ExpressionResult>()
                expression.branches.forEach { branch ->
                    branch.condition.accept(this, data)
                    val branchGraph = data.graph.clone()
                    val branchState = BuilderState(branchGraph, data.loop)
                    branchResults.add(ExpressionResult(branch.result.accept(this, branchState), branchGraph))
                }
                val isExhaustive = expression.branches.last().isUnconditional()
                require(isExhaustive || expression.type.isUnit())
                if (!isExhaustive) {
                    // Reflecting the case when none of the clauses have been executed.
                    branchResults.add(ExpressionResult(Node.Unit, data.graph))
                }
                return controlFlowMergePoint(data.graph, data.loop, expression, expression.type, branchResults)
            }

            val irBuilder = context.createIrBuilder(function.symbol)
            val fictitiousLoopsStarts = mutableMapOf<IrLoop, IrElement>()
            val loopsContinueResults = mutableMapOf<IrLoop, MutableList<ExpressionResult>>()
            val loopsBreakResults = mutableMapOf<IrLoop, MutableList<ExpressionResult>>()

            override fun visitContinue(jump: IrContinue, data: BuilderState): Node {
                (loopsContinueResults[jump.loop] ?: error("A continue from an unknown loop: ${jump.loop}"))
                        .add(ExpressionResult(Node.Unit, data.graph.clone()))
                // TODO: Return null (=Nothing) and clear the graph.
                return Node.Unit
            }

            override fun visitBreak(jump: IrBreak, data: BuilderState): Node {
                (loopsBreakResults[jump.loop] ?: error("A break from an unknown loop: ${jump.loop}"))
                        .add(ExpressionResult(Node.Unit, data.graph.clone()))
                // TODO: Return null (=Nothing) and clear the graph.
                return Node.Unit
            }

            override fun visitWhileLoop(loop: IrWhileLoop, data: BuilderState): Node {
                context.log { "before ${loop.dump()}" }
                data.graph.logDigraph(context)
                val fictitiousLoopStart = fictitiousLoopsStarts.getOrPut(loop) { irBuilder.irWhile() }
                val continueResults = loopsContinueResults.getOrPut(loop) { mutableListOf() }
                val breakResults = loopsBreakResults.getOrPut(loop) { mutableListOf() }

                for (node in data.graph.nodes) {
                    val objectNode = (node as? Node.Object)?.takeIf { it.loop == loop } ?: continue
                    objectNode.fields.values.forEach { it.pinnedPointsTo.clear() }
                }

                var prevGraph = data.graph
                loop.condition.accept(this, BuilderState(prevGraph, loop))
                val noIterationsGraph = data.graph.clone()
                var iterations = 0
                do {
                    context.log { "iter#$iterations:" }
                    prevGraph.logDigraph(context)
                    ++iterations
                    continueResults.clear()
                    breakResults.clear()
                    val curGraph = prevGraph.clone()
                    loop.body?.accept(this, BuilderState(curGraph, loop))
                    continueResults.add(ExpressionResult(Node.Unit, curGraph))
                    val nextGraph = PointsToGraph(prevGraph.forest)
                    controlFlowMergePoint(nextGraph, loop, fictitiousLoopStart, context.irBuiltIns.unitType, continueResults)
                    loop.condition.accept(this, BuilderState(nextGraph, loop))
                    val graphHasChanged = !forest.graphsAreEqual(prevGraph, nextGraph)
                    prevGraph = nextGraph
                } while (graphHasChanged && iterations < 10)

                if (iterations >= 10)
                    error("BUGBUGBUG: ${loop.dump()}")
                // A while loop might not execute even a single iteration.
                breakResults.add(ExpressionResult(Node.Unit, noIterationsGraph))
                breakResults.add(ExpressionResult(Node.Unit, prevGraph))
                controlFlowMergePoint(data.graph, data.loop, loop, context.irBuiltIns.unitType, breakResults)
                println("before adding pinned pointsTo edges")
                data.graph.logDigraph(context)

                for (node in data.graph.nodes) {
                    val objectNode = (node as? Node.Object)?.takeIf { it.loop == loop } ?: continue
                    for (fieldValue in objectNode.fields.values) {
                        data.graph.getObjectNodes(fieldValue, false, null, null)
                                .filterTo(fieldValue.pinnedPointsTo) { it.loop == loop }
                        if ((fieldValue.assignedWith as? Node.Object)?.loop == loop)
                            fieldValue.assignedWith = null
                    }
                }
                println("after ${loop.dump()}")
                data.graph.logDigraph(context)
                return Node.Unit
            }

            fun processCall(
                    // TODO: Must be different for each actual callee for a devirtualized call.
                    // Or may be create merged graph for all possible callees?
                    loop: IrLoop?,
                    callSite: IrFunctionAccessExpression,
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
                graph.logDigraph(context)
                context.log { "callee EA result" }
                calleeEscapeAnalysisResult.graph.logDigraph(context, calleeEscapeAnalysisResult.returnValue)

                val calleeGraph = calleeEscapeAnalysisResult.graph
                require(arguments.size == calleeGraph.parameterNodes.size)

                val referencesCount = IntArray(calleeGraph.nodes.size)
                for (node in calleeGraph.nodes) {
                    if (node !is Node.Reference) continue
                    when (node) {
                        is Node.Phi -> node.pointsTo.forEach { pointee ->
                            (pointee as? Node.Object)?.let { ++referencesCount[it.id] }
                        }
                        is Node.Variable -> {
                            (node.assignedWith as? Node.Object)?.let { ++referencesCount[it.id] }
                            (node as? Node.FieldValue)?.pinnedPointsTo?.forEach { ++referencesCount[it.id] }
                        }
                    }
                }

                val handledNodes = BitSet(calleeGraph.nodes.size)
                val inMirroredNodes = arrayOfNulls<Node>(calleeGraph.nodes.size) // Incoming edges.
                val outMirroredNodes = arrayOfNulls<Node>(calleeGraph.nodes.size) // Outgoing edges.

                fun reflectNode(node: Node, inMirroredNode: Node, outMirroredNode: Node = inMirroredNode) {
                    require(inMirroredNodes[node.id] == null) {
                        "Node $node has already been reflected to (${inMirroredNodes[node.id]} ${outMirroredNodes[node.id]})" +
                                " but is attempted to be reflected again to ($inMirroredNode $outMirroredNode)"
                    }
                    inMirroredNodes[node.id] = inMirroredNode
                    outMirroredNodes[node.id] = outMirroredNode
                }

                fun reflectFieldsOf(fictitiousObject: Node.Object, actualObjects: List<Node.Object>) {
                    class PossiblySplitMirroredNode() {
                        var inNode: Node.Phi? = null
                        var outNode: Node.VariableValue? = null

                        fun reflect(node: Node) {
                            require(inNode != null || outNode != null) { "Cannot reflect $node" }
                            reflectNode(node, inNode ?: outNode!!, outNode ?: inNode!!)
                        }
                    }

                    fictitiousObject.fields.forEach { (field, fieldValue) ->
                        val fieldPointee = fieldValue.assignedWith
                        if (fieldPointee == null && fieldValue.assignedTo.isEmpty())
                            require(fieldValue == calleeEscapeAnalysisResult.returnValue) {
                                "The node $fieldValue should've been optimized away"
                            }

                        val hasIncomingEdges = fieldValue.assignedTo.isNotEmpty()
                        val canOmitFictitiousObject = fieldPointee is Node.Object
                                && fieldPointee.isFictitious
                                && referencesCount[fieldPointee.id] == 1
                                && inMirroredNodes[fieldPointee.id] == null /* Skip loops */

                        val mirroredNode = if (actualObjects.size == 1)
                            null
                        else PossiblySplitMirroredNode().also {
                            if (hasIncomingEdges || fieldPointee == null)
                                it.inNode = graph.at(loop, callSite, fieldValue.id).newPhiNode()
                            if (fieldValue.pinnedPointsTo.isNotEmpty() || (fieldPointee != null && !canOmitFictitiousObject))
                                it.outNode = graph.at(loop, callSite, fieldValue.id + calleeGraph.nodes.size).newTempVariable()
                        }.takeIf { it.inNode != null || it.outNode != null }
                        mirroredNode?.reflect(fieldValue)

                        if (canOmitFictitiousObject) {
                            fieldPointee as Node.Object
//                            val mirroredNode = if (actualObjects.size == 1)
//                                null
//                            else {
//                                val inMirroredNode = if (hasIncomingEdges)
//                                    graph.at(loop, callSite, fieldValue.id).newPhiNode()
//                                else null
//                                val outMirroredNode = if (fieldValue.pinnedPointsTo.isEmpty())
//                                    null
//                                else graph.at(loop, callSite, fieldValue.id + calleeGraph.nodes.size).newTempVariable()
//                                PossiblySplitMirroredNode(inMirroredNode, outMirroredNode)
//                            }
//                            mirroredNode?.reflect(fieldValue)

//                            val mirroredNode = if (actualObjects.size > 1 && hasIncomingEdges)
//                                graph.at(loop, callSite, fieldValue.id).newPhiNode()
//                            else null
//                            mirroredNode?.let { reflectNode(fieldValue, it) }
                            val nextActualObjects = mutableListOf<Node.Object>()
                            for (obj in actualObjects) {
                                val objFieldValue = with(graph) { obj.getField(field) }
                                if (hasIncomingEdges || fieldValue.pinnedPointsTo.isNotEmpty()) {
                                    if (mirroredNode == null)
                                        reflectNode(fieldValue, objFieldValue)
                                    else {
                                        mirroredNode.outNode?.let { objFieldValue.addEdge(it) }
                                        mirroredNode.inNode?.addEdge(objFieldValue)
                                    }
//                                    if (mirroredNode == null)
//                                        reflectNode(fieldValue, objFieldValue)
//                                    else
//                                        mirroredNode.addEdge(objFieldValue)
                                }
                                nextActualObjects.addAll(
                                        graph.getObjectNodes(objFieldValue, true,
                                                fieldPointee.loop ?: loop, callSite, fieldValue.id + calleeGraph.nodes.size)
                                )
                            }
                            handledNodes.set(fieldValue.id)
                            reflectFieldsOf(fieldPointee, nextActualObjects)
                        } else {
//                            val mirroredNode = if (actualObjects.size == 1)
//                                null
//                            else {
//                                val inMirroredNode = if (fieldValue.assignedTo.isEmpty() && fieldPointee != null)
//                                    null
//                                else graph.at(loop, callSite, fieldValue.id).newPhiNode()
//                                val outMirroredNode = if (fieldPointee == null && fieldValue.pinnedPointsTo.isEmpty())
//                                    null
//                                else graph.at(loop, callSite, fieldValue.id + calleeGraph.nodes.size).newTempVariable()
//                                PossiblySplitMirroredNode(inMirroredNode, outMirroredNode)
//                            }
//                            mirroredNode?.reflect(fieldValue)
                            for (obj in actualObjects) {
                                val objFieldValue = with(graph) { obj.getField(field) }
                                if (fieldPointee != null) {
                                    // An actual field rewrite.
                                    if (objFieldValue.assignedTo.isNotEmpty())
                                        graph.bypass(objFieldValue, loop, callSite, fieldValue.id + 2 * calleeGraph.nodes.size)
                                    else
                                        objFieldValue.assignedWith = null
                                }

                                if (mirroredNode == null)
                                    reflectNode(fieldValue, objFieldValue)
                                else {
                                    mirroredNode.outNode?.let { objFieldValue.addEdge(it) }
                                    mirroredNode.inNode?.addEdge(objFieldValue)
                                }
                            }
                        }
                    }
                    handledNodes.set(fictitiousObject.id)
                }

                reflectNode(Node.Unit, Node.Unit)
                reflectNode(calleeEscapeAnalysisResult.graph.globalNode, graph.globalNode)
                for (parameter in calleeEscapeAnalysisResult.graph.parameterNodes.values)
                    reflectNode(parameter, arguments[parameter.index])
                for (parameter in calleeEscapeAnalysisResult.graph.parameterNodes.values)
                    reflectFieldsOf(parameter, graph.getObjectNodes(arguments[parameter.index], true, loop, callSite, parameter.id))

                for (node in calleeGraph.nodes) {
                    if (handledNodes.get(node!!.id) || inMirroredNodes[node.id] != null) continue
                    when (node) {
                        is Node.Parameter -> error("Parameter $node should've been handled earlier")
                        is Node.VariableValue -> {
                            require(node == calleeEscapeAnalysisResult.returnValue) { "All the variables should've been bypassed: $node" }
                            reflectNode(node, graph.at(loop, callSite, node.id).newTempVariable())
                        }
                        is Node.FieldValue -> Unit // Will be mirrored along with its owner object.
                        is Node.Phi -> reflectNode(node, graph.at(loop, callSite, node.id).newPhiNode())
                        is Node.Object -> {
                            val mirroredObject = graph.at(node.loop ?: loop, callSite, node.id).newObject(node.label)
                            reflectNode(node, mirroredObject)
                            node.fields.forEach { (field, fieldValue) ->
                                reflectNode(fieldValue, with(graph) { mirroredObject.getField(field) })
                            }
                        }
                    }
                }

                for (node in calleeGraph.nodes) {
                    if (handledNodes.get(node!!.id) || inMirroredNodes[node.id] != null) continue
                    require(node is Node.FieldValue) { "The node $node should've been reflected at this point" }
                    reflectNode(node, graph.at(loop, callSite, node.id).newTempVariable())
                }

                for (node in calleeGraph.nodes) {
                    if (node !is Node.FieldValue) continue
                    if (node.pinnedPointsTo.isEmpty()) continue

                    val reflectedPinnedObjects = node.pinnedPointsTo.map {
                        inMirroredNodes[it.id] as? Node.Object ?: error("Node $it should've been reflected to an object")
                    }
                    when (val reflectedNode = outMirroredNodes[node.id] as Node.Variable) {
                        is Node.FieldValue -> reflectedNode.pinnedPointsTo.addAll(reflectedPinnedObjects)
                        is Node.VariableValue -> reflectedNode.assignedTo.forEach {
                            (it as Node.FieldValue).pinnedPointsTo.addAll(reflectedPinnedObjects)
                        }
                    }
                }

                for (node in calleeGraph.nodes) {
                    if (handledNodes.get(node!!.id) || node !is Node.Reference) continue

                    val outMirroredNode = outMirroredNodes[node.id] as Node.Reference
                    when (node) {
                        is Node.Variable ->
                            node.assignedWith?.let {
                                outMirroredNode.addEdge(inMirroredNodes[it.id] ?: error("Node $it hasn't been reflected"))
                            }
                        is Node.Phi ->
                            node.pointsTo.forEach {
                                outMirroredNode.addEdge(inMirroredNodes[it.id] ?: error("Node $it hasn't been reflected"))
                            }
                    }
                }

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

                val returnValue = outMirroredNodes[calleeEscapeAnalysisResult.returnValue.id]
                        ?: error("Node ${calleeEscapeAnalysisResult.returnValue} hasn't been reflected")

                context.log { "after calling ${actualCallee.render()}" }
                graph.logDigraph(context, returnValue)

                return returnValue
            }

            fun processConstructorCall(
                    callee: IrConstructor,
                    thisNode: Node,
                    callSite: IrFunctionAccessExpression,
                    state: BuilderState
            ) {
                val arguments = callSite.getArgumentsWithIr()
                val argumentNodes = listOf(thisNode) + arguments.map { it.second.accept(this, state) }
                val calleeEscapeAnalysisResult = escapeAnalysisResults[callee.symbol]
                        ?: EscapeAnalysisResult.optimistic(context.irBuiltIns.unitType,
                                listOf(callee.constructedClass.thisReceiver!!) + arguments.map { it.first })
                processCall(state.loop, callSite, callee, argumentNodes, calleeEscapeAnalysisResult, state.graph)
            }

            fun BuilderState.allocObjectNode(expression: IrExpression, irClass: IrClass) =
                    graph.at(loop, expression).newObject("<A:${irClass.name}>")

            override fun visitConstructorCall(expression: IrConstructorCall, data: BuilderState): Node {
                val thisObject = data.allocObjectNode(expression, expression.symbol.owner.constructedClass)
                processConstructorCall(expression.symbol.owner, thisObject, expression, data)
                return thisObject
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: BuilderState): Node {
                val constructor = expression.symbol.owner
                val thisReceiver = (function as IrConstructor).constructedClass.thisReceiver!!
                val irThis = IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, thisReceiver.type, thisReceiver.symbol)
                processConstructorCall(constructor, irThis.accept(this, data), expression, data)
                return Node.Unit
            }

            val createUninitializedInstanceSymbol = context.ir.symbols.createUninitializedInstance
            val initInstanceSymbol = context.ir.symbols.initInstance

            override fun visitCall(expression: IrCall, data: BuilderState): Node {
                if (!expression.isVirtualCall) {
                    // TODO: What about staticInitializer, executeImpl, getContinuation?
                    return when (expression.symbol) {
                        createUninitializedInstanceSymbol -> {
                            data.allocObjectNode(expression, expression.getTypeArgument(0)!!.classOrNull!!.owner)
                        }
                        initInstanceSymbol -> {
                            val irThis = expression.getValueArgument(0)!!
                            val thisNode = irThis.accept(this, data)
                            val irInitializer = expression.getValueArgument(1) as IrConstructorCall
                            processConstructorCall(irInitializer.symbol.owner, thisNode, irInitializer, data)
                            Node.Unit
                        }
                        else -> {
                            val actualCallee = expression.actualCallee
                            val arguments = expression.getArgumentsWithIr()
                            val argumentNodes = arguments.map { it.second.accept(this, data) }
                            val calleeEscapeAnalysisResult = escapeAnalysisResults[actualCallee.symbol]
                                    ?: EscapeAnalysisResult.optimistic(actualCallee.returnType, arguments.map { it.first })
                            processCall(data.loop, expression, actualCallee, argumentNodes, calleeEscapeAnalysisResult, data.graph)
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

            context.log { "after analyzing body:" }
            functionResult.graph.logDigraph(context, functionResult.value)

            functionResult.graph.variableNodes.clear() // No need in this map anymore.
            functionResult.removeUnreachable()
            context.log { "after removing unreachable:" }
            functionResult.graph.logDigraph(context, functionResult.value)

            // TODO: Remove phi nodes with either one incoming or one outgoing edge.
            functionResult.bypassAndRemoveVariables()
            context.log { "after bypassing variables:" }
            functionResult.graph.logDigraph(context, functionResult.value)

            // Remove multi-edges.
            functionResult.graph.reenumerateNodes()
            val list = mutableListOf(functionResult.value)
            val graph = PointsToGraphForest(functionResult.graph.nodes.size).mergeGraphs(listOf(functionResult.graph), null, list)
            val returnValue = list[0]
            context.log { "after removing multi-edges:" }
            graph.logDigraph(context, returnValue)

            val optimizedFunctionResult = ExpressionResult(returnValue, graph)
            optimizedFunctionResult.removeRedundantNodes()
//            if (returnValue is Node.Reference) {
//                val isALeaf = when (returnValue) {
//                    is Node.Variable -> returnValue.assignedWith == null
//                    is Node.Phi -> returnValue.pointsTo.isEmpty()
//                }
//                if (isALeaf)
//                    returnValue.addEdge(optimizedFunctionResult.graph.constObjectNode(function.returnType))
//            }
            optimizedFunctionResult.graph.reenumerateNodes()
            context.log { "EA result for ${function.render()}" }
            optimizedFunctionResult.graph.logDigraph(context, optimizedFunctionResult.value)

            escapeAnalysisResults[function.symbol] = EscapeAnalysisResult(optimizedFunctionResult.graph, optimizedFunctionResult.value)
        }

        private fun PointsToGraph.reenumerateNodes() {
            var id = 0
            for (index in nodes.indices) {
                val node = nodes[index]
                if (node != null) {
                    nodes[id] = node
                    node.id = id++
                }
            }
            nodes.trimSize(id)
        }

        private fun ExpressionResult.removeUnreachable() {
            val nodeSet = BitSet(graph.forest.totalNodes)
            nodeSet.set(Node.UNIT_ID)

            fun findReachable(node: Node) {
                nodeSet.set(node.id)
                when (node) {
                    is Node.Object -> {
                        node.fields.values.forEach {
                            if (!nodeSet.get(it.id)) findReachable(it)
                        }
                    }
                    is Node.Variable -> {
                        node.assignedWith?.let {
                            if (!nodeSet.get(it.id)) findReachable(it)
                        }
                        (node as? Node.FieldValue)?.pinnedPointsTo?.forEach {
                            if (!nodeSet.get(it.id)) findReachable(it)
                        }
                    }
                    is Node.Phi -> {
                        node.pointsTo.forEach {
                            if (!nodeSet.get(it.id)) findReachable(it)
                        }
                    }
                }
            }

            if (value != Node.Unit)
                findReachable(value)
            findReachable(graph.globalNode)
            graph.parameterNodes.values.forEach {
                if (!nodeSet.get(it.id)) findReachable(it)
            }
            for (id in 0 until graph.nodes.size)
                if (!nodeSet.get(id))
                    graph.nodes[id] = null
        }

        private fun ExpressionResult.bypassAndRemoveVariables() {
//            val nodesCopy = graph.nodes.toMutableList()
//            graph.nodes.clear()
//
//            val nodeSet = BitSet(graph.forest.totalNodes)
//            nodeSet.set(value.id)
//            for (node in nodesCopy)
//                (node as? Node.Object)?.fields?.values?.forEach { nodeSet.set(it.id) }
//
//            nodesCopy.filterTo(graph.nodes) { node ->
//                (node as? Node.Variable)
//                        ?.takeIf { !nodeSet.get(node.id) }
//                        ?.also { variable ->
//                            require(variable.assignedTo.isNotEmpty())
//                            graph.bypass(variable, null)
//                        } == null
//            }
            val nodeSet = BitSet(graph.nodes.size)
            nodeSet.set(value.id)
            for (node in graph.nodes)
                (node as? Node.Object)?.fields?.values?.forEach { nodeSet.set(it.id) }

            for (id in 0 until graph.nodes.size) {
                val variable = graph.nodes[id] as? Node.Variable ?: continue
                if (nodeSet.get(id)) continue
                require(variable.assignedTo.isNotEmpty())
                graph.bypass(variable, null, null)
                graph.nodes[id] = null
            }
        }

        private fun ExpressionResult.removeRedundantNodes() {
            val singleNodesPointingAt = arrayOfNulls<Node>(graph.nodes.size)
            val incomingEdgesCounts = IntArray(graph.nodes.size)

            fun checkIncomingEdge(from: Node, toId: Int) {
                singleNodesPointingAt[toId] = from.takeIf { ++incomingEdgesCounts[toId] == 1 }
            }

            val leaves = mutableListOf<Node>()
            for (node in graph.nodes) {
                require(node != null)
                when (node) {
                    is Node.Object -> node.fields.values.let { fieldValues ->
                        if (fieldValues.isEmpty())
                            leaves.add(node)
                        else
                            fieldValues.forEach { checkIncomingEdge(node, it.id) }
                    }
                    is Node.VariableValue -> node.assignedWith.let { assignedWith ->
                        if (assignedWith == null)
                            leaves.add(node)
                        else
                            checkIncomingEdge(node, assignedWith.id)
                    }
                    is Node.FieldValue -> {
                        val assignedWith = node.assignedWith
                        assignedWith?.let { checkIncomingEdge(node, it.id) }
                        node.pinnedPointsTo.forEach { checkIncomingEdge(node, it.id) }
                        if (assignedWith == null && node.pinnedPointsTo.isEmpty())
                            leaves.add(node)
                    }
                    is Node.Phi -> node.pointsTo.let { pointsTo ->
                        if (pointsTo.isEmpty())
                            leaves.add(node)
                        else
                            pointsTo.forEach { checkIncomingEdge(node, it.id) }
                    }
                }
            }

            val forbiddenToRemove = BitSet(graph.nodes.size)
            val removed = BitSet(graph.nodes.size)
            forbiddenToRemove.set(Node.UNIT_ID)
            forbiddenToRemove.set(Node.GLOBAL_ID)
            forbiddenToRemove.set(value.id)
            graph.parameterNodes.values.forEach { forbiddenToRemove.set(it.id) }
            while (leaves.isNotEmpty()) {
                val leafNode = leaves.pop()
                val leafNodeId = leafNode.id
                if (forbiddenToRemove.get(leafNodeId)) continue
                val singleNodePointingAtLeaf = singleNodesPointingAt[leafNodeId] ?: continue
                removed.set(leafNodeId)
                val hasBecomeALeaf = when (singleNodePointingAtLeaf) {
                    is Node.Object -> singleNodePointingAtLeaf.fields.let { fields ->
                        fields.remove((leafNode as Node.FieldValue).field)
                        fields.isEmpty()
                    }
                    is Node.Variable -> {
                        singleNodePointingAtLeaf.assignedWith = null
                        true
                    }
                    is Node.Phi -> singleNodePointingAtLeaf.pointsTo.let { pointsTo ->
                        pointsTo.remove(leafNode)
                        pointsTo.isEmpty()
                    }
                }
                if (hasBecomeALeaf)
                    leaves.push(singleNodePointingAtLeaf)
            }

//            @Suppress("UnnecessaryVariable") val nodesCopy = singleNodesPointingAt
//            for (node in graph.nodes)
//                nodesCopy[node!!.id] = node.takeIf { !removed.get(node.id) }
//            graph.nodes.clear()
//            nodesCopy.filterNotNullTo(graph.nodes)
            for (id in 0 until graph.nodes.size)
                if (removed.get(id))
                    graph.nodes[id] = null
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
