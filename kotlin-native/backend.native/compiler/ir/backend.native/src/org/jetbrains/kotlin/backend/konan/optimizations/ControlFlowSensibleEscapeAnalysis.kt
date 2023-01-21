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
import org.jetbrains.kotlin.backend.konan.lower.erasedUpperBound
import org.jetbrains.kotlin.ir.IrElement
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

        private class EscapeAnalysisResult(val graph: PointsToGraph, val returnValue: Node.VariableValue) {
            companion object {
                fun optimistic(returnType: IrType, parameters: List<IrValueParameter>): EscapeAnalysisResult {
                    val pointsToGraph = PointsToGraph(PointsToGraphForest())
                    parameters.forEachIndexed { index, parameter -> pointsToGraph.addParameter(parameter, index) }
                    val returnValue = with(pointsToGraph) {
                        newTempVariable().also { it.addEdge(if (returnType.isUnit()) Node.Unit else constObjectNode(returnType)) }
                    }
                    return EscapeAnalysisResult(pointsToGraph, returnValue)
                }
            }
        }

        private sealed class Node(var id: Int) {
//            @Suppress("LeakingThis")
//            var mirroredNode = this

            abstract fun shallowCopy(): Node

            // TODO: Do we need to distinguish fictitious objects and materialized ones?
            open class Object(id: Int, val label: String? = null) : Node(id) {
                val fields = mutableMapOf<IrFieldSymbol, FieldValue>()

                val isFictitious get() = label == null

                override fun shallowCopy() = Object(id, label)
                override fun toString() = "${label ?: "D"}$id"
            }

            class Parameter(id: Int, val index: Int, val irValueParameter: IrValueParameter) : Object(id) {
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

            class FieldValue(id: Int, val field: IrFieldSymbol) : Variable(id) {
                override fun shallowCopy() = FieldValue(id, field)
                override fun toString() = "F$id"
            }

            class VariableValue(id: Int, val irVariable: IrVariable? = null) : Variable(id) {
                override fun shallowCopy() = VariableValue(id, irVariable)
                override fun toString() = irVariable?.let { "<V:${it.name}>$id" } ?: "T$id"
            }

            object Unit : Object(UNIT_ID, "<U>") {
                override fun shallowCopy() = this
            }

            companion object {
                const val UNIT_ID = 0
                const val GLOBAL_ID = 1
                const val LOWEST_NODE_ID = 2
            }
        }

        private class PointsToGraphForest {
            private var currentNodeId = Node.LOWEST_NODE_ID
            fun nextNodeId() = currentNodeId++
            val totalNodes get() = currentNodeId

            private val fieldIds = mutableMapOf<Pair<Int, IrFieldSymbol>, Int>()
            fun getFieldId(objectId: Int, field: IrFieldSymbol) = fieldIds.getOrPut(Pair(objectId, field)) { nextNodeId() }

            fun cloneGraph(graph: PointsToGraph, otherNodesToMap: MutableList<Node>): PointsToGraph {
                val newNodes = arrayOfNulls<Node?>(totalNodes)
                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
                val variableNodes = mutableMapOf<IrVariable, Node.VariableValue>()
                for (node in graph.nodes)
                    newNodes[node.id] = node.shallowCopy().also { copy ->
                        (copy as? Node.Parameter)?.irValueParameter?.let { parameterNodes[it] = copy }
                        (copy as? Node.VariableValue)?.irVariable?.let { variableNodes[it] = copy }
                    }
                for (node in graph.nodes) {
                    when (val newNode = newNodes[node.id]!!) {
                        is Node.Object -> {
                            (node as Node.Object).fields.entries.forEach { (field, fieldNode) ->
                                newNode.fields[field] = newNodes[fieldNode.id] as Node.FieldValue
                            }
                        }
                        is Node.Variable -> {
                            newNode.assignedWith = (node as Node.Variable).assignedWith?.let { newNodes[it.id]!! }
                            node.assignedTo.mapTo(newNode.assignedTo) { newNodes[it.id] as Node.Reference }
                        }
                        is Node.Phi -> {
                            (node as Node.Phi).pointsTo.mapTo(newNode.pointsTo) { newNodes[it.id]!! }
                        }
                    }
                }
                for (i in otherNodesToMap.indices)
                    otherNodesToMap[i] = newNodes[otherNodesToMap[i].id]!!
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

            fun mergeGraphs(graphs: List<PointsToGraph>, otherNodesToMap: MutableList<Node>): PointsToGraph {
                // TODO: Nothing to do if graphs.size == 1
                val newNodes = arrayOfNulls<Node?>(totalNodes)
                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
                val variableNodes = mutableMapOf<IrVariable, Node.VariableValue>()
                var edgesCount = 0
                graphs.flatMap { it.nodes }.forEach { node ->
                    (node as? Node.Phi)?.let { edgesCount += it.pointsTo.size }
                    (node as? Node.Variable)?.let { if (it.assignedWith != null) ++edgesCount }
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

                val newPhiNodes = mutableListOf<Node.Phi>()
                val edges = LongArray(makePrime(5 * edgesCount))
                graphs.flatMap { it.nodes }.forEach { node ->
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
                            val prevAssignedWith = newNode.assignedWith
                            val assignedWith = (node as Node.Variable).assignedWith ?: return@forEach
                            if (prevAssignedWith == null)
                                newNode.assignedWith = newNodes[assignedWith.id]!!
                            else if (assignedWith.id != prevAssignedWith.id) {
                                val phiNode = if (prevAssignedWith.id >= newNodes.size)
                                    prevAssignedWith as Node.Phi
                                else Node.Phi(nextNodeId()).also {
                                    newPhiNodes += it
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
                val nodes = ArrayList<Node>(newNodes.size + newPhiNodes.size)
                for (node in newNodes) {
                    nodes += node ?: continue
                    ((node as? Node.Variable)?.assignedWith as? Node.Variable)?.assignedTo?.add(node)
                    (node as? Node.Phi)?.pointsTo?.forEach { (it as? Node.Variable)?.assignedTo?.add(node) }
                }
                nodes.addAll(newPhiNodes)
                return PointsToGraph(this, nodes, parameterNodes, variableNodes)
            }
        }

        private class PointsToGraph(
                private val forest: PointsToGraphForest,
                val nodes: MutableList<Node>,
                val parameterNodes: MutableMap<IrValueParameter, Node.Parameter>,
                val variableNodes: MutableMap<IrVariable, Node.VariableValue>
        ) {
            constructor(forest: PointsToGraphForest)
                    : this(forest, mutableListOf(Node.Unit, Node.Object(Node.GLOBAL_ID, "<G>")), mutableMapOf(), mutableMapOf())

            private var _globalNode = nodes.first { it.id == Node.GLOBAL_ID } as Node.Object
            val globalNode get() = _globalNode

            fun copyFrom(other: PointsToGraph) {
                nodes.clear()
                nodes.addAll(other.nodes)
                other.parameterNodes.forEach { (parameter, parameterNode) -> parameterNodes[parameter] = parameterNode }
                other.variableNodes.forEach { (variable, variableNode) -> variableNodes[variable] = variableNode }
                _globalNode = other.globalNode
            }

            fun addParameter(parameter: IrValueParameter, index: Int): Node.Parameter {
                val parameterNode = Node.Parameter(forest.nextNodeId(), index, parameter)
                parameterNodes[parameter] = parameterNode.also { nodes += it }
                return parameterNode
            }

            fun addVariable(irVariable: IrVariable): Node.VariableValue {
                val variableNode = Node.VariableValue(forest.nextNodeId(), irVariable)
                variableNodes[irVariable] = variableNode.also { nodes += it }
                return variableNode
            }

            fun newTempVariable() = Node.VariableValue(forest.nextNodeId()).also { nodes += it }
            fun newPhiNode() = Node.Phi(forest.nextNodeId()).also { nodes += it }
            fun newObject(label: String? = null) = Node.Object(forest.nextNodeId(), label).also { nodes += it }

            fun constObjectNode(irType: IrType) = newObject("<C:${irType.erasedUpperBound.name}>")
            fun allocObjectNode(irClass: IrClass) = newObject("<A:${irClass.name}>")

            fun Node.Object.getField(field: IrFieldSymbol) =
                    fields.getOrPut(field) { Node.FieldValue(forest.getFieldId(id, field), field).also { nodes += it } }

            fun Node.Variable.addObjectNodeIfLeaf() {
                if (assignedWith == null)
                    assignedWith = newObject()
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
            fun bypass(v: Node.Variable) {
                val w = v.assignedWith ?: newObject()
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

            fun getObjectNodes(node: Node): List<Node.Object> = when (node) {
                is Node.Object -> listOf(node)
                is Node.Reference -> {
                    val reachable = mutableSetOf<Node.Reference>()

                    fun findReachable(node: Node.Reference) {
                        reachable.add(node)
                        when (node) {
                            is Node.Variable ->
                                (node.assignedWith as? Node.Reference)?.let {
                                    if (it !in reachable) findReachable(it)
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
                                is Node.Variable -> {
                                    reachableNode.addObjectNodeIfLeaf()
                                    (reachableNode.assignedWith as? Node.Object)?.let { add(it) }
                                }
                                is Node.Phi -> {
                                    for (pointee in reachableNode.pointsTo)
                                        (pointee as? Node.Object)?.let { add(it) }
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
                nodes.forEach { +"    ${it.format()}" }
                +"edges:"
                nodes.forEach { node ->
                    when (node) {
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

            fun logDigraph(context: Context) = context.logMultiple {
                log(context)

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

                    +"$name[label=\"$this\" shape=${if (this is Node.Object) "rect" else "oval"}]"
                    return name
                }

                val vertices = nodes.associateWith { it.format() }

                nodes.forEach { node ->
                    val vertex = vertices[node]!!
                    when (node) {
                        is Node.Object ->
                            node.fields.forEach { (field, fieldValue) -> +"$vertex -> ${vertices[fieldValue] ?: error("No node ${fieldValue.id} in graph")}[label=\"${field.name}\"];" }
                        is Node.Variable ->
                            node.assignedWith?.let { +"$vertex -> ${vertices[it] ?: error("No node ${it.id} in graph")};" }
                        is Node.Phi ->
                            node.pointsTo.forEach { +"$vertex -> ${vertices[it] ?: error("No node ${it.id} in graph")};" }
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
                val pointsToGraph = PointsToGraph(forest)
                function.allParameters.forEachIndexed { index, parameter -> pointsToGraph.addParameter(parameter, index) }
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
                graph.logDigraph(context)

                // TODO: uncomment.
                //val mergedGraph = if (results.size == 1) results[0].graph else forest.mergeGraphs(results.map { it.graph })
                val resultNodes = results.map { it.value }.toMutableList()
                val mergedGraph = forest.mergeGraphs(results.map { it.graph }, resultNodes)
                graph.copyFrom(mergedGraph)
                return if (type.isUnit())
                    Node.Unit
                else graph.newPhiNode().also { phiNode ->
                    resultNodes.forEach { phiNode.addEdge(it) }
                }.also {

                    context.log { "after CFG merge" }
                    graph.logDigraph(context)
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
                    IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> data.constObjectNode(expression.type)
                    else -> error("Not expected: ${expression.operator}")
                }
            }

            override fun visitConst(expression: IrConst<*>, data: PointsToGraph) =
                    data.constObjectNode(expression.type)

            override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: PointsToGraph) = Node.Unit

            override fun visitGetValue(expression: IrGetValue, data: PointsToGraph) = when (val owner = expression.symbol.owner) {
                is IrValueParameter -> data.parameterNodes[owner] ?: error("Unknown value parameter: ${owner.render()}")
                is IrVariable -> data.variableNodes[owner] ?: error("Unknown variable: ${owner.render()}")
                else -> error("Unknown value declaration: ${owner.render()}")
            }

            override fun visitSetValue(expression: IrSetValue, data: PointsToGraph): Node {
                context.log { "before ${expression.dump()}" }
                data.logDigraph(context)

                val variable = expression.symbol.owner as IrVariable
                val valueNode = expression.value.accept(this, data)
                val variableNode = data.variableNodes[variable] ?: error("Unknown variable: ${variable.render()}")
                if (variableNode.assignedTo.isNotEmpty())
                    data.bypass(variableNode)
                else
                    variableNode.assignedWith = null
                variableNode.addEdge(valueNode)

                context.log { "after ${expression.dump()}" }
                data.logDigraph(context)

                return Node.Unit
            }

            override fun visitVariable(declaration: IrVariable, data: PointsToGraph): Node {
                context.log { "before ${declaration.dump()}" }
                data.logDigraph(context)

                val valueNode = declaration.initializer?.accept(this, data)

                val variableNode = data.addVariable(declaration)
                valueNode?.let { variableNode.addEdge(it) }

                context.log { "after ${declaration.dump()}" }
                data.logDigraph(context)

                return Node.Unit
            }

            override fun visitGetField(expression: IrGetField, data: PointsToGraph): Node = with(data) {
                context.log { "before ${expression.dump()}" }
                data.logDigraph(context)

                val receiverNode = expression.receiver?.accept(this@PointsToGraphBuilder, data) ?: data.globalNode
                val receiverObjects = getObjectNodes(receiverNode)

                context.log { "after getting receivers" }
                data.logDigraph(context)
                context.logMultiple {
                    +"receivers:"
                    receiverObjects.forEach { +"    ${it.id}" }
                }

                return (if (receiverObjects.size == 1)
                    receiverObjects[0].getField(expression.symbol)
                else newPhiNode().also { phiNode ->
                    for (receiver in receiverObjects)
                        phiNode.addEdge(receiver.getField(expression.symbol))
                }).also {

                    context.log { "after ${expression.dump()}" }
                    data.logDigraph(context)
                }
            }

            override fun visitSetField(expression: IrSetField, data: PointsToGraph): Node = with(data) {
                context.log { "before ${expression.dump()}" }
                data.logDigraph(context)

                val receiverNode = expression.receiver?.accept(this@PointsToGraphBuilder, data) ?: data.globalNode
                val receiverObjects = getObjectNodes(receiverNode)

                context.log { "after getting receivers" }
                data.logDigraph(context)
                context.logMultiple {
                    +"receivers:"
                    receiverObjects.forEach { +"    ${it.id}" }
                }

                val valueNode = expression.value.accept(this@PointsToGraphBuilder, data)

                context.log { "after evaluating value" }
                data.logDigraph(context)

                receiverObjects.forEach {
                    val fieldNode = it.getField(expression.symbol)
                    if (fieldNode.assignedTo.isNotEmpty())
                        bypass(fieldNode)
                    else
                        fieldNode.assignedWith = null
                    fieldNode.addEdge(valueNode)
                }

                context.log { "after ${expression.dump()}" }
                data.logDigraph(context)

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
                graph.logDigraph(context)
                context.log { "callee EA result" }
                calleeEscapeAnalysisResult.graph.logDigraph(context)

                val calleeGraph = calleeEscapeAnalysisResult.graph
                require(arguments.size == calleeGraph.parameterNodes.size)

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
                    class PossiblySplitMirroredNode(val inNode: Node.Phi?, val outNode: Node.VariableValue?) {
                        fun reflect(node: Node) {
                            reflectNode(node, inNode ?: outNode!!, outNode ?: inNode!!)
                        }
                    }

                    fictitiousObject.fields.forEach { (field, fieldValue) ->
                        val fieldPointee = fieldValue.assignedWith
                        if (fieldPointee == null && fieldValue.assignedTo.isEmpty())
                            return@forEach // TODO: Such node actually should've been optimized away.

                        if ((fieldPointee as? Node.Object)?.isFictitious == true
                                && inMirroredNodes[fieldPointee.id] == null /* Skip loops */) {
                            val mirroredNode = if (actualObjects.size == 1) null else graph.newPhiNode()
                            mirroredNode?.let { reflectNode(fieldValue, it) }
                            val nextActualObjects = mutableListOf<Node.Object>()
                            for (obj in actualObjects) {
                                val objFieldValue = with(graph) { obj.getField(field) }
                                if (mirroredNode == null)
                                    reflectNode(fieldValue, objFieldValue)
                                else
                                    mirroredNode.addEdge(objFieldValue)
                                nextActualObjects.addAll(graph.getObjectNodes(objFieldValue))
                            }
                            handledNodes.set(fieldValue.id)
                            reflectFieldsOf(fieldPointee, nextActualObjects)
                        } else {
                            val mirroredNode = if (actualObjects.size == 1)
                                null
                            else {
                                val inMirroredNode = if (fieldValue.assignedTo.isEmpty()) null else graph.newPhiNode()
                                val outMirroredNode = fieldPointee?.let { graph.newTempVariable() }
                                PossiblySplitMirroredNode(inMirroredNode, outMirroredNode)
                            }
                            mirroredNode?.reflect(fieldValue)
                            for (obj in actualObjects) {
                                val objFieldValue = with(graph) { obj.getField(field) }
                                if (fieldPointee != null) {
                                    // An actual field rewrite.
                                    if (objFieldValue.assignedTo.isNotEmpty())
                                        graph.bypass(objFieldValue)
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

                val returnNode = graph.newTempVariable()
//                val returnNode = when {
//                    actualCallee is IrConstructor || actualCallee.returnType.isUnit() -> Node.Unit
//                    else -> graph.newTemporary()
//                }
                reflectNode(calleeEscapeAnalysisResult.returnValue, returnNode)
                reflectNode(Node.Unit, Node.Unit)
                reflectNode(calleeEscapeAnalysisResult.graph.globalNode, graph.globalNode)
                for (parameter in calleeEscapeAnalysisResult.graph.parameterNodes.values)
                    reflectNode(parameter, arguments[parameter.index])
                for (parameter in calleeEscapeAnalysisResult.graph.parameterNodes.values)
                    reflectFieldsOf(parameter, graph.getObjectNodes(arguments[parameter.index]))

//                val argumentsObjects = arguments.map { graph.getObjectNodes(it) }



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

                for (node in calleeGraph.nodes) {
                    if (handledNodes.get(node.id) || inMirroredNodes[node.id] != null) continue
                    when (node) {
                        is Node.Parameter -> error("Parameter $node should've been handled earlier")
                        is Node.VariableValue -> error("All the variables should've been bypassed: $node")
                        is Node.FieldValue -> Unit // Will be mirrored along with its owner object.
                        is Node.Phi -> reflectNode(node, graph.newPhiNode())
                        is Node.Object -> {
                            val mirroredObject = graph.newObject(node.label)
                            reflectNode(node, mirroredObject)
                            node.fields.forEach { (field, fieldValue) ->
                                reflectNode(fieldValue, with(graph) { mirroredObject.getField(field) })
                            }
                        }
                    }
                }

                for (node in calleeGraph.nodes) {
                    if (handledNodes.get(node.id) || inMirroredNodes[node.id] != null) continue
                    require(node is Node.FieldValue) { "The node $node should've been reflected at this point" }
                    reflectNode(node, graph.newTempVariable())
                }

                for (node in calleeGraph.nodes) {
                    if (handledNodes.get(node.id) || node !is Node.Reference) continue

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
                graph.logDigraph(context)

                return returnNode
            }

            fun processConstructorCall(
                    callee: IrConstructor,
                    thisNode: Node,
                    callSite: IrFunctionAccessExpression,
                    graph: PointsToGraph
            ) {
                val arguments = callSite.getArgumentsWithIr()
                val argumentNodes = listOf(thisNode) + arguments.map { it.second.accept(this, graph) }
                val calleeEscapeAnalysisResult = escapeAnalysisResults[callee.symbol]
                        ?: EscapeAnalysisResult.optimistic(context.irBuiltIns.unitType,
                                listOf(callee.constructedClass.thisReceiver!!) + arguments.map { it.first })
                processCall(callee, argumentNodes, calleeEscapeAnalysisResult, graph)
            }

            override fun visitConstructorCall(expression: IrConstructorCall, data: PointsToGraph): Node {
                val thisObject = data.allocObjectNode(expression.symbol.owner.constructedClass)
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
                        createUninitializedInstanceSymbol -> {
                            data.allocObjectNode(expression.getTypeArgument(0)!!.classOrNull!!.owner)
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
                            processCall(actualCallee, argumentNodes, calleeEscapeAnalysisResult, data)
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

            context.log { "after analyzing body:" }
            functionResult.graph.logDigraph(context)

            val returnValue = functionResult.graph.newTempVariable()
            returnValue.addEdge(functionResult.value)
            val reachable = mutableSetOf<Node>(Node.Unit)
            findReachable(returnValue, reachable)
            findReachable(functionResult.graph.globalNode, reachable)
            functionResult.graph.parameterNodes.values.forEach {
                if (it !in reachable) findReachable(it, reachable)
            }

            context.log { "after removing unreachable:" }
            PointsToGraph(forest, reachable.toMutableList(), functionResult.graph.parameterNodes, mutableMapOf()).logDigraph(context)

            // TODO: bypass phi nodes with either single incoming edge or single outgoing edge.
            val nodes = mutableListOf<Node>()
            reachable.forEach { node ->
                if (node !is Node.VariableValue || node == returnValue)
                    nodes.add(node)
                else {
                    if (node.assignedTo.isNotEmpty())
                        functionResult.graph.bypass(node)
                    else
                        node.assignedWith = null
                }
            }

            context.log { "after bypassing variables:" }
            PointsToGraph(forest, nodes, functionResult.graph.parameterNodes, mutableMapOf()).logDigraph(context)
//            val nodes = reachable.toMutableList()

            var index = Node.LOWEST_NODE_ID
            nodes.forEach { node ->
                if (node != Node.Unit && node != functionResult.graph.globalNode)
                    node.id = index++
            }

            context.log { "after reenumerating:" }
            PointsToGraph(forest, nodes, functionResult.graph.parameterNodes, mutableMapOf()).logDigraph(context)

            val list = mutableListOf<Node>(returnValue)
            // Remove multi-edges.
            val graph = forest.mergeGraphs(listOf(PointsToGraph(forest, nodes, functionResult.graph.parameterNodes, mutableMapOf())), list)

            // TODO: Remove redundant fields and objects.

            context.log { "EA result for ${function.render()}" }
            graph.logDigraph(context)

            escapeAnalysisResults[function.symbol] = EscapeAnalysisResult(graph, list[0] as Node.VariableValue)
        }

        private fun findReachable(node: Node, visited: MutableSet<Node>) {
            visited.add(node)
            when (node) {
                is Node.Object -> {
                    node.fields.values.forEach {
                        if (it !in visited) findReachable(it, visited)
                    }
                }
                is Node.Variable -> {
                    node.assignedWith?.let {
                        if (it !in visited) findReachable(it, visited)
                    }
                }
                is Node.Phi -> {
                    node.pointsTo.forEach {
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
