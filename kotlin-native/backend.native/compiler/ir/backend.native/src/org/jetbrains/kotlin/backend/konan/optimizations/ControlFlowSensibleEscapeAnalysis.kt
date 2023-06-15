/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DirectedGraphCondensationBuilder
import org.jetbrains.kotlin.backend.konan.DirectedGraphMultiNode
import org.jetbrains.kotlin.backend.konan.ir.actualCallee
import org.jetbrains.kotlin.backend.konan.ir.isVirtualCall
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.logMultiple
import org.jetbrains.kotlin.backend.konan.lower.erasedUpperBound
import org.jetbrains.kotlin.backend.konan.lower.isStaticInitializer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.removeAll

private fun <T> MutableList<T>.removeAll(element: T) {
    var i = 0
    for (j in indices) {
        val item = this[j]
        if (item != element)
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

internal object ControlFlowSensibleEscapeAnalysis {
    private val escapesField = IrFieldSymbolImpl()
    private val intestinesField = IrFieldSymbolImpl()

    private val IrFieldSymbol.name: String
        get() = when {
            this == escapesField -> "<escapes>"
            this == intestinesField -> "<intestines>"
            isBound -> owner.name.asString()
            else -> "<unbound>"
        }

    private class InterproceduralAnalysis(
            val context: Context,
            generationState: NativeGenerationState,
            val callGraph: CallGraph,
            val moduleDFG: ModuleDFG,
            val needDebug: (IrFunction) -> Boolean
    ) {
        @Suppress("UNUSED_PARAMETER")
        fun analyze(lifetimes: MutableMap<IrElement, Lifetime>) {
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
            for (multiNode in condensation.topologicalOrder.reversed()) {
                val currentLifetimes = mutableMapOf<IrFunctionAccessExpression, Lifetime>()
                val allocationToFunction = mutableMapOf<IrFunctionAccessExpression, IrFunction>()
                analyze(multiNode, escapeAnalysisResults, currentLifetimes, allocationToFunction)
                currentLifetimes.forEach { (ir, lifetime) ->
                    lifetimes[ir] = lifetime
//                    val expectedLifetime = lifetimes[ir]
//                    if (expectedLifetime == Lifetime.STACK && lifetime == Lifetime.GLOBAL) {
//                        error("BUGBUGBUG: ${allocationToFunction[ir]!!.render()}\n${ir.dump()}")
//                        //println("BUGBUGBUG: ${allocationToFunction[ir]!!.render()}\n${ir.dump()}")
//                        //println()
//                    }
////                    if (expectedLifetime == Lifetime.GLOBAL && lifetime == Lifetime.STACK) {
////                        println("YEAH, BABY: ${allocationToFunction[ir]!!.render()}\n${ir.dump()}")
////                        println()
////                    }
                }
            }
        }

        private enum class ComputationState {
            NEW,
            PENDING,
            DONE
        }

        object DivergenceResolutionParams {
            const val MaxAttempts = 3
            const val NegligibleSize = 100
            const val SwellingFactor = 25
        }

        // TODO: Compute from IR.
        private fun maxPointsToGraphSizeOf(function: DataFlowIR.FunctionSymbol) = with(DivergenceResolutionParams) {
            // A heuristic: the majority of functions have their points-to graph size linear to the number of IR (or DFG) nodes,
            // there are exceptions, but it's a trade-off we have to make.
            // The trick with [NegligibleSize] handles functions that basically delegate their work to other functions.
            val numberOfNodes = moduleDFG.functions[function]!!.body.allScopes.sumOf { it.nodes.size }
            NegligibleSize + numberOfNodes * SwellingFactor
        }

        private fun analyze(
                multiNode: DirectedGraphMultiNode<DataFlowIR.FunctionSymbol.Declared>,
                escapeAnalysisResults: MutableMap<IrFunctionSymbol, EscapeAnalysisResult>,
                lifetimes: MutableMap<IrFunctionAccessExpression, Lifetime>,
                allocationToFunction: MutableMap<IrFunctionAccessExpression, IrFunction>,
        ) {
            val nodes = multiNode.nodes.filter { callGraph.directEdges.containsKey(it) && it.irFunction != null }.toMutableSet()

            nodes.forEach {
                val function = it.irFunction!!
                escapeAnalysisResults[function.symbol] = EscapeAnalysisResult.optimistic(function)
            }

            context.logMultiple {
                +"Analyzing multiNode:\n    ${multiNode.nodes.joinToString("\n   ") { it.toString() }}"
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

            var failedToConverge = false
            val toAnalyze = mutableSetOf<DataFlowIR.FunctionSymbol.Declared>()
            toAnalyze.addAll(nodes)
            val numberOfRuns = nodes.associateWith { 0 }.toMutableMap()
            while (!failedToConverge && toAnalyze.isNotEmpty()) {
                val node = toAnalyze.first()
                toAnalyze.remove(node)
                val function = node.irFunction!!
                numberOfRuns[node] = numberOfRuns[node]!! + 1
                context.log { "Processing function $node" }

                val startResult = escapeAnalysisResults[function.symbol]!!
                context.log { "Start escape analysis result:" }
                startResult.logDigraph(context)

//                val pointsToGraph = PointsToGraph(node)
//                pointsToGraphs[node] = pointsToGraph

                if (!intraproceduralAnalysis(
                                callGraph.directEdges[node]!!,
                                escapeAnalysisResults,
                                lifetimes,
                                allocationToFunction,
                                maxPointsToGraphSizeOf(node))
                ) {
                    failedToConverge = true
                } else {
                    val endResult = escapeAnalysisResults[function.symbol]!!
                    if (startResult == endResult) {
                        context.log { "Escape analysis is not changed" }
                    } else {
                        context.log { "Escape analysis was refined:" }
                        endResult.logDigraph(context)
                        if (numberOfRuns[node]!! > DivergenceResolutionParams.MaxAttempts)
                            failedToConverge = true
                        else {
                            callGraph.reversedEdges[node]?.forEach {
                                if (nodes.contains(it))
                                    toAnalyze.add(it)
                            }
                        }
                    }
                }

                if (failedToConverge)
                    context.log { "WARNING: Escape analysis for $node seems not to be converging. Falling back to conservative strategy." }
            }

            if (failedToConverge) {
                /*pointsToGraphs = */analyzePessimistically(multiNode, escapeAnalysisResults, lifetimes, allocationToFunction)
            }
        }

        private fun analyzePessimistically(
                multiNode: DirectedGraphMultiNode<DataFlowIR.FunctionSymbol.Declared>,
                escapeAnalysisResults: MutableMap<IrFunctionSymbol, EscapeAnalysisResult>,
                lifetimes: MutableMap<IrFunctionAccessExpression, Lifetime>,
                allocationToFunction: MutableMap<IrFunctionAccessExpression, IrFunction>,
        ) {
            val nodes = multiNode.nodes.filter { callGraph.directEdges.containsKey(it) }.toMutableSet()
            //val pointsToGraphs = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, InterproceduralAnalysis.PointsToGraph>()
            val computationStates = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, ComputationState>()
            nodes.forEach { computationStates[it] = ComputationState.NEW }
            val toAnalyze = nodes.toMutableList()
            while (toAnalyze.isNotEmpty()) {
                val node = toAnalyze.peek()!!
                val function = node.irFunction!!
                val state = computationStates[node]!!
                val callGraphNode = callGraph.directEdges[node]!!
                val callSites = callGraphNode.callSites
                when (state) {
                    ComputationState.NEW -> {
                        computationStates[node] = ComputationState.PENDING
                        for (callSite in callSites) {
                            val next = callSite.actualCallee
                            val calleeComputationState = computationStates[next]
                            if (callSite.isVirtual
                                    || next !is DataFlowIR.FunctionSymbol.Declared // An external call.
                                    || calleeComputationState == null // A call to a function from other component.
                                    || calleeComputationState == ComputationState.DONE // Already analyzed.
                            ) {
                                continue
                            }

                            if (calleeComputationState == ComputationState.PENDING) {
                                // A cycle - break it by assuming nothing about the callee.
                                // This is not the callee's final result - it will be recomputed later in the loop.
                                val callee = next.irFunction ?: error("No IR for $next")
                                escapeAnalysisResults[callee.symbol] = EscapeAnalysisResult.pessimistic(callee)
                            } else {
                                computationStates[next] = ComputationState.NEW
                                toAnalyze.push(next)
                            }
                        }
                    }

                    ComputationState.PENDING -> {
                        toAnalyze.pop()
                        computationStates[node] = ComputationState.DONE
//                        val pointsToGraph = PointsToGraph(node)
                        if (intraproceduralAnalysis(
                                        callGraphNode,
                                        escapeAnalysisResults,
                                        lifetimes,
                                        allocationToFunction,
                                        maxPointsToGraphSizeOf(node))
                        ) {
                            //pointsToGraphs[function] = pointsToGraph
                        } else {
                            // TODO: suboptimal. May be it is possible somehow handle the entire component at once?
                            context.log {
                                "WARNING: Escape analysis for $node seems not to be converging." +
                                        " Assuming conservative results."
                            }
                            escapeAnalysisResults[function.symbol] = EscapeAnalysisResult.pessimistic(function)
                            // Invalidate the points-to graph.
//                            pointsToGraphs[function] = PointsToGraph(function).apply {
//                                allNodes.forEach { it.depth = EscapeAnalysis.Depths.GLOBAL }
//                            }
                        }
                    }

                    ComputationState.DONE -> {
                        toAnalyze.pop()
                    }
                }
            }

//            return pointsToGraphs
        }

        private class EscapeAnalysisResult(val graph: PointsToGraph, val returnValue: Node, val objectsReferencedFromThrown: BitSet) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is EscapeAnalysisResult) return false
                return returnValue.id == other.returnValue.id
                        && objectsReferencedFromThrown == other.objectsReferencedFromThrown
                        && PointsToGraphForest.graphsAreEqual(graph, other.graph)
            }

            fun logDigraph(context: Context) {
                graph.logDigraph(context) {
                    thrownNodes = objectsReferencedFromThrown
                    +returnValue
                }
            }

            fun lifetimeOf(node: Node.Object) = node.forcedLifetime
                    ?: if (objectsReferencedFromThrown.get(node.id))
                        Lifetime.GLOBAL
                    else when (node.kind) {
                        PointsToGraphNodeKind.GLOBAL -> Lifetime.GLOBAL

                        PointsToGraphNodeKind.PARAMETER -> Lifetime.ARGUMENT

                        PointsToGraphNodeKind.STACK -> {
                            // A value doesn't escape from its scope - it can be allocated on the stack.
                            Lifetime.STACK
                        }

                        PointsToGraphNodeKind.LOCAL -> {
                            // A value is neither stored into a global nor into any parameter nor into the return value -
                            // it can be allocated locally.
                            Lifetime.LOCAL
                        }

                        PointsToGraphNodeKind.RETURN_VALUE -> {
                            Lifetime.RETURN_VALUE
                        }
                    }

            companion object {
                fun optimistic(function: IrFunction): EscapeAnalysisResult {
                    val pointsToGraph = PointsToGraph(PointsToGraphForest(function), function)
                    val returnValue = with(pointsToGraph) {
                        when {
                            function.returnType.isNothing() -> Node.Nothing
                            function is IrConstructor || function.returnType.isUnit() -> Node.Unit
                            else -> newTempVariable("RET@${function.name}")
                        }
                    }
                    return EscapeAnalysisResult(pointsToGraph, returnValue, BitSet())
                }

                fun pessimistic(function: IrFunction): EscapeAnalysisResult {
                    val result = optimistic(function)
                    with(result.graph) {
                        val globalEscapes = globalNode.getField(escapesField)
                        parameterNodes.values.forEach { globalEscapes.addEdge(it) }
                        (result.returnValue as? Node.Variable)?.let { globalEscapes.addEdge(it) }
                    }
                    return result
                }

                fun fromBits(function: IrFunction, escapesMask: Int, pointsToMasks: List<Int>): EscapeAnalysisResult {
                    val result = optimistic(function)
                    var returnObject: Node.Object? = null
                    val parameterNodes = arrayOfNulls<Node.Parameter>(result.graph.parameterNodes.size)
                    result.graph.parameterNodes.values.forEach { parameterNodes[it.index] = it }

                    fun getIntestines(index: Int): Node.FieldValue {
                        val obj = if (index < parameterNodes.size)
                            parameterNodes[index]!!
                        else {
                            returnObject ?: result.graph.newObject().also {
                                returnObject = it
                                (result.returnValue as Node.Variable).addEdge(it)
                            }
                        }
                        return with(result.graph) { obj.getField(intestinesField) }
                    }

                    if (escapesMask != 0) {
                        val globalEscapes = with(result.graph) { globalNode.getField(escapesField) }
                        for (parameterNode in parameterNodes) {
                            if (escapesMask and (1 shl parameterNode!!.index) != 0)
                                globalEscapes.addEdge(parameterNode)
                        }
                        if (escapesMask and (1 shl parameterNodes.size) != 0)
                            (result.returnValue as? Node.Variable)?.let { globalEscapes.addEdge(it) }
                    }
                    pointsToMasks.forEachIndexed { fromIndex, mask ->
                        for (toIndex in pointsToMasks.indices) {
                            // Read a nibble at position [toIndex].
                            val pointsToKind = (mask shr (4 * toIndex)) and 15
                            require(pointsToKind <= 4) { "Invalid pointsTo kind $pointsToKind" }
                            if (pointsToKind == 0) continue
                            val fromVariable = when {
                                pointsToKind >= 3 -> getIntestines(fromIndex)
                                fromIndex == parameterNodes.size -> result.returnValue as Node.Variable
                                else -> error("A parameter can point to something only through its fields")
                            }
                            val toNode = when {
                                pointsToKind % 2 == 0 -> getIntestines(toIndex)
                                toIndex == parameterNodes.size -> result.returnValue
                                else -> parameterNodes[toIndex]!!
                            }
                            fromVariable.addEdge(toNode)
                        }
                    }
                    return result
                }
            }
        }

        // The less is the value, the higher an object escapes.
        private object Levels {
            val FUNCTION = 0
            val RETURN_VALUE = -1
            val PARAMETER = -2
            val GLOBAL = -3
        }

        private enum class PointsToGraphNodeKind {
            STACK,
            LOCAL,
            PARAMETER,
            RETURN_VALUE,
            GLOBAL
        }

        private sealed class Node(var id: Int, val level: Int) {
            abstract fun shallowCopy(): Node

            var actualLevel = level

            val kind
                get() = when {
                    actualLevel == Levels.GLOBAL -> PointsToGraphNodeKind.GLOBAL
                    actualLevel == Levels.PARAMETER -> PointsToGraphNodeKind.PARAMETER
                    actualLevel == Levels.RETURN_VALUE -> PointsToGraphNodeKind.RETURN_VALUE
                    actualLevel != level -> PointsToGraphNodeKind.LOCAL
                    else -> PointsToGraphNodeKind.STACK
                }

            // TODO: Do we need to distinguish fictitious objects and materialized ones?
            open class Object(id: Int, level: Int, var loop: IrLoop?, val label: String? = null) : Node(id, level) {
                val fields = mutableMapOf<IrFieldSymbol, FieldValue>()

                val isFictitious get() = label == null

                var forcedLifetime: Lifetime? = null

                override fun shallowCopy() = Object(id, level, loop, label)
                override fun toString() = "${label ?: "D"}$id"
            }

            class Parameter(id: Int, val index: Int, val irValueParameter: IrValueParameter) : Object(id, Levels.PARAMETER, null) {
                override fun shallowCopy() = Parameter(id, index, irValueParameter)
                override fun toString() = "<P:${irValueParameter.name}[$index]>$id"
            }

            sealed class Reference(id: Int, level: Int) : Node(id, level) {
                // TODO: Rename?
                val assignedWith = mutableListOf<Node>()
                val assignedTo = mutableListOf<Reference>()

                fun addEdge(to: Node) {
                    assignedWith.add(to)
                    (to as? Reference)?.assignedTo?.add(this)
                }
            }

            class FieldValue(id: Int, level: Int, val ownerId: Int, val field: IrFieldSymbol) : Reference(id, level) {
                override fun shallowCopy() = FieldValue(id, level, ownerId, field)
                override fun toString() = "F$id"
            }

            class Variable(id: Int, level: Int, val irVariable: IrVariable?, val label: String) : Reference(id, level) {
                constructor(id: Int, level: Int, irVariable: IrVariable) : this(id, level, irVariable, "<V:${irVariable.name}>")
                constructor(id: Int, level: Int, label: String) : this(id, level, null, label)

                override fun shallowCopy() = Variable(id, level, irVariable, label)
                override fun toString() = "$label$id"
            }

            // TODO: Make separate inheritor (SpecialObject isn't allowed to have any fields, force this with type hierarchy).
            open class SpecialObject(id: Int, label: String) : Object(id, Levels.GLOBAL, null, label) {
                override fun toString() = label!!
                override fun shallowCopy() = this
            }

            object Nothing : SpecialObject(NOTHING_ID, "⊥")
            object Null : SpecialObject(NULL_ID, "NULL")
            object Unit : SpecialObject(UNIT_ID, "( )")

            inline fun forEachPointee(block: (Node) -> kotlin.Unit) {
                when (this) {
                    is Object -> this.fields.values.forEach { block(it) }
                    is Reference -> this.assignedWith.forEach { block(it) }
                }
            }

            companion object {
                const val NOTHING_ID = 0
                const val NULL_ID = 1
                const val UNIT_ID = 2
                const val GLOBAL_ID = 3
                const val LOWEST_NODE_ID = 4
            }
        }

        private class PointsToGraphForest(startNodeId: Int) {
            constructor(function: IrFunction) : this(Node.LOWEST_NODE_ID + function.allParametersCount)

            private var currentNodeId = startNodeId
            fun nextNodeId() = currentNodeId++
            val totalNodes get() = currentNodeId

            // TODO: Optimize.
            private val ids = mutableMapOf<Pair<Int, Any>, Int>()
            fun getAssociatedId(nodeId: Int, obj: Any) = ids.getOrPut(Pair(nodeId, obj)) { nextNodeId() }

            fun cloneGraph(graph: PointsToGraph, otherNodesToMap: MutableList<Node>): PointsToGraph {
                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
                val variableNodes = mutableMapOf<IrVariable, Node.Variable>()
                val newNodes = ArrayList<Node?>(graph.nodes.size)
                graph.nodes.mapTo(newNodes) { node ->
                    node?.shallowCopy()?.also { copy ->
                        (copy as? Node.Parameter)?.irValueParameter?.let { parameterNodes[it] = copy }
                        (copy as? Node.Variable)?.irVariable?.let { variableNodes[it] = copy }
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
                        is Node.Reference -> {
                            (node as Node.Reference).assignedWith.mapTo(newNode.assignedWith) { newNodes[it.id]!! }
                            node.assignedTo.mapTo(newNode.assignedTo) { newNodes[it.id] as Node.Reference }
                        }
                    }
                }
                for (i in otherNodesToMap.indices)
                    otherNodesToMap[i] = newNodes[otherNodesToMap[i].id]!!
                return PointsToGraph(this, newNodes, parameterNodes, variableNodes)
            }

            inner class GraphBuilder(function: IrFunction, startEdgesCount: Int = 10) {
                val nodes = ArrayList<Node?>(totalNodes).also {
                    it.addAll(arrayOf(Node.Nothing, Node.Null, Node.Unit, Node.Object(Node.GLOBAL_ID, Levels.GLOBAL, null, "G")))
                    it.ensureSize(totalNodes)
                }
                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
                val variableNodes = mutableMapOf<IrVariable, Node.Variable>()

                // TODO: Merge somehow with the same code from PointsToGraph.
                init {
                    function.allParameters.forEachIndexed { index, parameter ->
                        val id = Node.LOWEST_NODE_ID + index
                        require(id < totalNodes)
                        val parameterNode = Node.Parameter(id, index, parameter)
                        parameterNodes[parameter] = parameterNode
                        nodes[id] = parameterNode
                    }
                }

                var edgesCount = 0
                var bagOfEdges = LongArray(makePrime(5 * startEdgesCount))
                var modCount = 0

                val isEmpty get() = modCount == 0

                fun build() = PointsToGraph(this@PointsToGraphForest, nodes, parameterNodes, variableNodes)

                fun growIfNeeded() {
                    if (bagOfEdges.size < 3 * edgesCount) {
                        val newBagOfEdges = LongArray(makePrime(5 * edgesCount))
                        for (edge in bagOfEdges) {
                            if (edge != 0L)
                                addEdge(edge, newBagOfEdges)
                        }
                        bagOfEdges = newBagOfEdges
                    }
                }

                fun merge(graph: PointsToGraph) {
                    var somethingChanged = false
                    nodes.ensureSize(totalNodes)
                    for (node in graph.nodes) {
                        val id = node?.id ?: continue
                        if (nodes[id] == null) {
                            somethingChanged = true
                            nodes[id] = node.shallowCopy().also { copy ->
                                (copy as? Node.Parameter)?.irValueParameter?.let { parameterNodes[it] = copy }
                                (copy as? Node.Variable)?.irVariable?.let { variableNodes[it] = copy }
                            }
                        }
                    }

                    for (node in graph.nodes) {
                        val id = node?.id ?: continue
                        when (val mappedNode = nodes[id]!!) {
                            is Node.Object -> {
                                (node as Node.Object).fields.forEach { (field, fieldNode) ->
                                    mappedNode.fields.getOrPut(field) {
                                        somethingChanged = true
                                        nodes[fieldNode.id] as Node.FieldValue
                                    }
                                }
                            }
                            is Node.Reference -> {
                                (node as Node.Reference).assignedWith.forEach {
                                    if (addEdge(node.id, it.id, bagOfEdges)) {
                                        somethingChanged = true
                                        ++edgesCount
                                        mappedNode.addEdge(nodes[it.id]!!)
                                        growIfNeeded()
                                    }
                                }
                            }
                        }
                    }

                    if (somethingChanged)
                        ++modCount
                }
            }

            fun addEdge(from: Int, to: Int, bagOfEdges: LongArray): Boolean =
                    addEdge(from.toLong() or (to.toLong() shl 32), bagOfEdges)

            fun addEdge(edge: Long, bagOfEdges: LongArray): Boolean {
                // This is 64-bit extension of a hashing method from Knuth's "The Art of Computer Programming".
                // The magic constant is the closest prime to 2^64 * phi, where phi is the golden ratio.
                var index = ((edge.toULong() * 11400714819323198393UL) % bagOfEdges.size.toULong()).toInt()
                while (bagOfEdges[index] != 0L && bagOfEdges[index] != edge) {
                    ++index
                    if (index == bagOfEdges.size) index = 0
                }
                if (bagOfEdges[index] != 0L) return false
                bagOfEdges[index] = edge
                return true
            }

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
                val newNodes = ArrayList<Node?>(totalNodes).also { it.ensureSize(totalNodes) }
                val parameterNodes = mutableMapOf<IrValueParameter, Node.Parameter>()
                val variableNodes = mutableMapOf<IrVariable, Node.Variable>()
                var edgesCount = 0
                graphs.flatMap { it.nodes }.forEach { node ->
                    if (node == null) return@forEach
                    (node as? Node.Reference)?.let { edgesCount += it.assignedWith.size }
                    if (newNodes[node.id] == null)
                        newNodes[node.id] = node.shallowCopy().also { copy ->
                            (copy as? Node.Parameter)?.irValueParameter?.let { parameterNodes[it] = copy }
                            (copy as? Node.Variable)?.irVariable?.let { variableNodes[it] = copy }
                        }
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
                        is Node.Reference -> {
                            (node as Node.Reference).assignedWith.forEach {
                                if (addEdge(node.id, it.id, edges))
                                    newNode.addEdge(newNodes[it.id]!!)
                            }
                        }
                    }
                }

                for (i in otherNodesToMap.indices)
                    otherNodesToMap[i] = newNodes[otherNodesToMap[i].id]!!
                return PointsToGraph(this, newNodes, parameterNodes, variableNodes)
            }

            companion object {
                fun graphsAreEqual(first: PointsToGraph, second: PointsToGraph): Boolean {
                    val size = kotlin.math.min(first.nodes.size, second.nodes.size)
                    for (id in size until first.nodes.size)
                        if (first.nodes[id] != null) return false
                    for (id in size until second.nodes.size)
                        if (second.nodes[id] != null) return false
                    val firstPointsTo = BitSet(size)
                    val secondPointsTo = BitSet(size)
                    for (id in 0 until size) {
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
                            is Node.Reference -> {
                                if (secondNode !is Node.Reference) return false
                                if (firstNode::class.java != secondNode::class.java) return false
                                firstNode.assignedWith.forEach { firstPointsTo.set(it.id) }
                                secondNode.assignedWith.forEach { secondPointsTo.set(it.id) }
                                firstNode.assignedWith.forEach { if (!secondPointsTo.get(it.id)) return false }
                                secondNode.assignedWith.forEach { if (!firstPointsTo.get(it.id)) return false }
                                firstNode.assignedWith.forEach { firstPointsTo.clear(it.id) }
                                secondNode.assignedWith.forEach { secondPointsTo.clear(it.id) }
                            }
                        }
                    }

                    return true
                }
            }
        }

        private interface NodeFactory {
            fun newTempVariable(label: String = "T"): Node.Variable
            fun newObject(label: String? = null): Node.Object
        }

        private fun NodeFactory.newPhiNode() = newTempVariable("φ")

        private data class NodeContext(val level: Int, val anchorIds: Boolean, val loop: IrLoop?, val element: IrElement?)

        private class PointsToGraph(
                val forest: PointsToGraphForest,
                val nodes: MutableList<Node?>,
                val parameterNodes: MutableMap<IrValueParameter, Node.Parameter>,
                val variableNodes: MutableMap<IrVariable, Node.Variable>
        ) : NodeFactory {
            constructor(forest: PointsToGraphForest, function: IrFunction)
                    : this(forest,
                    mutableListOf(
                            Node.Nothing, Node.Null, Node.Unit, Node.Object(Node.GLOBAL_ID, Levels.GLOBAL, null, "G")
                    ),
                    mutableMapOf(), mutableMapOf()
            ) {
                function.allParameters.forEachIndexed { index, parameter ->
                    val id = Node.LOWEST_NODE_ID + index
                    require(id < forest.totalNodes)
                    val parameterNode = Node.Parameter(id, index, parameter)
                    parameterNodes[parameter] = parameterNode
                    nodes.add(parameterNode)
                }
            }

            private inline fun <reified T : Node> getOrPutNodeAt(id: Int, nodeBuilder: (Int) -> T): T {
                nodes.ensureSize(id + 1)
                nodes[id]?.let { return it as? T ?: error("Node $it is expected to be of type ${T::class.java}") }
                return nodeBuilder(id).also { nodes[id] = it }
            }

            private inline fun <reified T : Node> putNewNodeAt(id: Int, nodeBuilder: (Int) -> T): T {
                nodes.ensureSize(id + 1)
                val node = nodeBuilder(id)
                require(nodes[id] == null) { "Duplicate node at $id: ${nodes[id]} and $node" }
                nodes[id] = node
                return node
            }

            private inner class AnchoredNodeFactory(var currentNodeId: Int, val level: Int, val loop: IrLoop?) : NodeFactory {
                private fun getNodeId(): Int {
                    require(currentNodeId != -1) { "Anchored node factory can only create one node" }
                    return currentNodeId.also { currentNodeId = -1 }
                }

                override fun newTempVariable(label: String) = getOrPutNodeAt(getNodeId()) { Node.Variable(it, level, label) }
                override fun newObject(label: String?) = getOrPutNodeAt(getNodeId()) { Node.Object(it, level, loop, label) }
            }

            val globalNode get() = nodes[Node.GLOBAL_ID] as Node.Object

            fun copyFrom(other: PointsToGraph) {
                nodes.clear()
                nodes.addAll(other.nodes)
                other.parameterNodes.forEach { (parameter, parameterNode) -> parameterNodes[parameter] = parameterNode }
                other.variableNodes.forEach { (variable, variableNode) -> variableNodes[variable] = variableNode }
            }

            fun clear() {
                globalNode.fields.clear()
                for (id in Node.LOWEST_NODE_ID until nodes.size) {
                    val node = nodes[id] ?: continue
                    when {
                        node is Node.Variable && node.irVariable != null -> {
                            node.assignedWith.clear()
                            node.assignedTo.clear()
                        }
                        node is Node.Parameter -> node.fields.clear()
                        else -> nodes[id] = null
                    }
                }
            }

            fun getOrAddVariable(irVariable: IrVariable, level: Int) = variableNodes.getOrPut(irVariable) {
                putNewNodeAt(forest.getAssociatedId(0, irVariable)) { Node.Variable(it, level, irVariable) }
            }

            override fun newTempVariable(label: String) = getOrPutNodeAt(forest.nextNodeId()) {
                Node.Variable(it, Levels.FUNCTION, label)
            }

            override fun newObject(label: String?) = getOrPutNodeAt(forest.nextNodeId()) {
                Node.Object(it, Levels.FUNCTION, null, label)
            }

            fun at(nodeContext: NodeContext, anchorNodeId: Int = 0): NodeFactory = with(nodeContext) {
                if (anchorIds)
                    AnchoredNodeFactory(forest.getAssociatedId(anchorNodeId, element!!), level, loop)
                else
                    this@PointsToGraph
            }

            fun Node.Object.getField(field: IrFieldSymbol): Node.FieldValue {
                require(this !is Node.SpecialObject)
                return fields.getOrPut(field) {
                    putNewNodeAt(forest.getAssociatedId(id, field)) { Node.FieldValue(it, level, id, field) }
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
            fun bypass(v: Node.Reference, nodeContext: NodeContext, anchorNodeId: Int = 0) {
                require(v.assignedTo.isNotEmpty())
                val assignedWith = v.assignedWith.ifEmpty { listOf(at(nodeContext, anchorNodeId).newObject()) }
                        .filter { it != v }
                val assignedTo = v.assignedTo.filter { it != v }
                for (u in assignedTo) {
                    u.assignedWith.removeAll(v)
                    u.assignedWith.addAll(assignedWith)
                }
                assignedWith.forEach { w ->
                    (w as? Node.Reference)?.assignedTo?.run {
                        removeAll(v)
                        addAll(assignedTo)
                    }
                }
                v.assignedWith.clear()
                v.assignedTo.clear()
            }

            // Called only for get/set field of [node] further.
            fun getObjectNodes(node: Node, nodeContext: NodeContext, anchorNodeId: Int = 0) = when (node) {
                is Node.SpecialObject -> emptyList()
                is Node.Object -> listOf(node)
                is Node.Reference -> {
                    val visited = BitSet()
                    // Skip null, as getting/setting its field would lead to a segfault
                    // (not NPE, which is being thrown explicitly by !! operator).
                    visited.set(Node.NULL_ID)
                    visited.set(Node.UNIT_ID) // Unit might get here because of generics and non-exact devirtualization.
                    val reachable = mutableListOf<Node.Reference>()

                    fun findReachable(node: Node.Reference) {
                        visited.set(node.id)
                        reachable.add(node)
                        for (pointee in node.assignedWith) {
                            if (pointee is Node.Reference && !visited.get(pointee.id))
                                findReachable(pointee)
                        }
                    }

                    findReachable(node)
                    buildList {
                        fun tryAddObject(obj: Node.Object) {
                            if (!visited.get(obj.id)) {
                                visited.set(obj.id)
                                add(obj)
                            }
                        }

                        for (reachableNode in reachable) {
                            if (reachableNode.assignedWith.isNotEmpty()) {
                                reachableNode.assignedWith.forEach { pointee ->
                                    (pointee as? Node.Object)?.let { tryAddObject(it) }
                                }
                            } else {
                                val fictitiousObject = at(nodeContext, anchorNodeId).newObject()
                                reachableNode.assignedWith.add(fictitiousObject)
                                tryAddObject(fictitiousObject)
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
                nodes.forEach { node -> node?.let { +"    ${it.format()} ${it.actualLevel}" } }
                +"edges:"
                nodes.forEach { node ->
                    when (node) {
                        null -> return@forEach
                        is Node.Object -> {
                            node.fields.forEach { (field, fieldValue) ->
                                +"    ${node.format()} ---> ${fieldValue.format()}[field=${field.name}]"
                            }
                        }
                        is Node.Reference -> {
                            node.assignedWith.forEach { +"    ${node.format()} ${it.formatIncomingEdge()} ${it.format()}" }
                            node.assignedTo.forEach { +"    ${node.format()} <-*- ${it.format()}" }
                        }
                    }
                }
            }

            class LoggingOptions(val highlightLifetimes: Boolean, val markedNodes: BitSet, val thrownNodes: BitSet)

            class LoggingOptionsBuilder {
                var highlightLifetimes = false
                val markedNodes = BitSet()
                var thrownNodes = BitSet()

                operator fun Node.unaryPlus() = markedNodes.set(this.id)

                fun build() = LoggingOptions(highlightLifetimes, markedNodes, thrownNodes)
            }

            fun logDigraph(context: Context, block: LoggingOptionsBuilder.() -> Unit) = context.logMultiple {
//                log(context)

                val builder = LoggingOptionsBuilder()
                builder.block()
                val options = builder.build()

                +"digraph {"
                +"rankdir=\"LR\";"

                fun Node.format(): String {
                    val name = when (this) {
                        Node.Nothing -> "nothing"
                        Node.Null -> "null"
                        Node.Unit -> "unit"
                        globalNode -> "global"
                        is Node.Parameter -> "param$id"
                        is Node.Object -> "obj$id"
                        is Node.FieldValue -> "field$id"
                        is Node.Variable -> "var$id"
                    }

                    val label = "label=\"$this\""
                    val shape = " shape=${if (this is Node.Object) "rect" else "oval"}"
                    val colors = buildString {
                        if (options.highlightLifetimes) {
                            when ((this@format as? Node.Object)?.forcedLifetime) {
                                Lifetime.STACK -> append(" penwidth=3.0 color=green")
                                Lifetime.GLOBAL -> append(" penwidth=3.0 color=red")
                                else -> {}
                            }
                            if (options.markedNodes.get(this@format.id))
                                append(" fillcolor=yellow style=filled")
                        } else {
                            if ((this@format as? Node.Object)?.loop != null)
                                append(" penwidth=3.0 color=deepskyblue")
                            val referencedFromThrown = options.thrownNodes.get(this@format.id)
                            val marked = options.markedNodes.get(this@format.id)
                            when {
                                referencedFromThrown && marked -> append(" fillcolor=orangered style=filled")
                                referencedFromThrown -> append(" fillcolor=crimson style=filled")
                                marked -> append(" fillcolor=yellow style=filled")
                            }
                        }
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
                        is Node.Reference ->
                            node.assignedWith.forEach { +"$vertex -> ${vertices[it] ?: error("No node ${it.id} in graph")};" }
                    }
                }

                +"}"
            }
        }

        private class ExpressionResult(val value: Node, val graph: PointsToGraph)
        private class MultipleExpressionResult(val valueIds: BitSet, val graphBuilder: PointsToGraphForest.GraphBuilder) {
            fun merge(value: Node, graph: PointsToGraph) {
                if (value == Node.Nothing) return
                valueIds.set(value.id)
                graphBuilder.merge(graph)
            }
        }

        private data class BuilderState(val graph: PointsToGraph, val level: Int, val anchorIds: Boolean, val loop: IrLoop?, val tryBlock: IrTry?) {
            fun toNodeContext(element: IrElement?) = NodeContext(level, anchorIds, loop, element)
        }

        private data class PointsToGraphBuilderResult(
                val escapeAnalysisResult: EscapeAnalysisResult,
                val allocations: Map<IrFunctionAccessExpression, Int>
        )

        private inner class PointsToGraphBuilder(
                val function: IrFunction,
                val forest: PointsToGraphForest,
                val escapeAnalysisResults: Map<IrFunctionSymbol, EscapeAnalysisResult>,
                val devirtualizedCallSites: Map<IrCall, List<IrFunctionSymbol>>,
                val maxAllowedGraphSize: Int,
                val needDebug: Boolean,
        ) : IrElementVisitor<Node, BuilderState> {

            private inline fun debug(block: () -> Unit) =
                    if (needDebug) block() else Unit

            val irBuilder = context.createIrBuilder(function.symbol)
            val fictitiousVariableInitSetValues = mutableMapOf<IrVariable, IrSetValue>()
            val devirtualizedFictitiousCallSites = mutableMapOf<IrCall, List<IrFunctionAccessExpression>>()

            val returnTargetResults = mutableMapOf<IrReturnTargetSymbol, MultipleExpressionResult>()
            val tryBlocksThrowGraphs = mutableMapOf<IrTry, PointsToGraphForest.GraphBuilder>()
            val loopsContinueGraphs = mutableMapOf<IrLoop, PointsToGraphForest.GraphBuilder>()
            val loopsBreakGraphs = mutableMapOf<IrLoop, PointsToGraphForest.GraphBuilder>()
            val objectsReferencedFromThrown = BitSet()
            val allocations = mutableMapOf<IrFunctionAccessExpression, Int>()

            fun build(): PointsToGraphBuilderResult {
                require(Node.Nothing.fields.isEmpty())
                require(Node.Null.fields.isEmpty())
                require(Node.Unit.fields.isEmpty())
                val pointsToGraph = PointsToGraph(forest, function)
                val functionResult = MultipleExpressionResult(BitSet(), forest.GraphBuilder(function))
                returnTargetResults[function.symbol] = functionResult
                val state = BuilderState(pointsToGraph, Levels.FUNCTION, false, null, null)
                (function.body as IrBlockBody).statements.forEach { it.accept(this, state) }
                if (forest.totalNodes > maxAllowedGraphSize)
                    return PointsToGraphBuilderResult(EscapeAnalysisResult.pessimistic(function), emptyMap())
                if (functionResult.valueIds.isEmpty) // Function returns Nothing.
                    functionResult.valueIds.set(Node.NOTHING_ID)
                val functionResultNode = controlFlowMergePoint(pointsToGraph, state.toNodeContext(null), function.returnType, functionResult)
                return PointsToGraphBuilderResult(
                        EscapeAnalysisResult(pointsToGraph, functionResultNode, objectsReferencedFromThrown),
                        allocations
                )
            }

            fun PointsToGraph.logDigraph(vararg markedNodes: Node) =
                    logDigraph(context) {
                        thrownNodes = objectsReferencedFromThrown
                        for (node in markedNodes) +node
                    }

            fun PointsToGraph.logDigraph(context: Context, markedNodes: List<Node>) =
                    logDigraph(context) {
                        thrownNodes = objectsReferencedFromThrown
                        for (node in markedNodes) +node
                    }

            fun PointsToGraph.unreachable(): Node {
                clear()
                return Node.Nothing
            }

            fun controlFlowMergePoint(graph: PointsToGraph, nodeContext: NodeContext, type: IrType, results: List<ExpressionResult>) =
                    controlFlowMergePointImpl(graph, nodeContext, type, results.filterNot { it.value == Node.Nothing })

            fun controlFlowMergePointImpl(
                    graph: PointsToGraph, nodeContext: NodeContext,
                    type: IrType, results: List<ExpressionResult>
            ): Node = when (results.size) {
                0 -> {
                    graph.unreachable()
                }
                1 -> {
                    graph.copyFrom(results[0].graph)
                    when {
                        type.isNothing() -> Node.Nothing
                        type.isUnit() -> Node.Unit
                        else -> results[0].value
                    }
                }
                else -> {
                    debug {
                        context.log { "before CFG merge" }
                        results.forEachIndexed { index, result ->
                            context.log { "#$index:" }
                            result.graph.logDigraph(result.value)
                        }
                    }
                    val resultNodes = results.map { it.value }.toMutableList()
                    val mergedGraph = forest.mergeGraphs(results.map { it.graph }, resultNodes)
                    graph.copyFrom(mergedGraph)
                    when {
                        type.isNothing() -> Node.Nothing
                        type.isUnit() -> Node.Unit
                        else -> graph.at(nodeContext).newPhiNode().also { phiNode ->
                            resultNodes.forEach { phiNode.addEdge(it) }
                        }
                    }.also {
                        debug {
                            context.log { "after CFG merge" }
                            graph.logDigraph(it)
                        }
                    }
                }
            }

            fun controlFlowMergePoint(
                    graph: PointsToGraph, nodeContext: NodeContext,
                    type: IrType, multipleResult: MultipleExpressionResult
            ): Node {
                if (multipleResult.valueIds.isEmpty)
                    return graph.unreachable()
                graph.copyFrom(multipleResult.graphBuilder.build())
                return when {
                    type.isNothing() -> Node.Nothing
                    type.isUnit() -> Node.Unit
                    else -> {
                        val results = mutableListOf<Node>()
                        multipleResult.valueIds.forEachBit { results.add(graph.nodes[it]!!) }
                        if (results.size == 1)
                            results[0]
                        else {
                            graph.at(nodeContext).newPhiNode().also { phiNode ->
                                results.forEach { phiNode.addEdge(it) }
                            }
                        }
                    }
                }
            }

            inline fun <T : IrElement> checkGraphSizeAndVisit(element: T, state: BuilderState, visit: () -> Node): Node {
                if (state.graph.forest.totalNodes > maxAllowedGraphSize) {
                    context.log { "The graph is bigger than expected - skipping ${element.render()}" }
                    return state.graph.unreachable()
                }
                return visit()
            }

            inline fun <T : IrElement> PointsToGraph.checkSizeAndVisit(element: T, visit: PointsToGraph.() -> Node): Node {
                if (forest.totalNodes > maxAllowedGraphSize) {
                    context.log { "The graph is bigger than expected - skipping ${element.render()}" }
                    return unreachable()
                }
                return visit()
            }

            override fun visitElement(element: IrElement, data: BuilderState): Node = TODO(element.render())
            override fun visitExpression(expression: IrExpression, data: BuilderState): Node = TODO(expression.render())
            override fun visitDeclaration(declaration: IrDeclarationBase, data: BuilderState): Node = TODO(declaration.render())

            fun BuilderState.constObjectNode(expression: IrExpression) =
                    graph.at(toNodeContext(expression)).newObject("<C:${expression.type.erasedUpperBound.name}>")

            override fun visitConst(expression: IrConst<*>, data: BuilderState) =
                    expression.value?.let { data.constObjectNode(expression) } ?: Node.Null
            override fun visitConstantValue(expression: IrConstantValue, data: BuilderState) = data.constObjectNode(expression)
            override fun visitFunctionReference(expression: IrFunctionReference, data: BuilderState) = data.constObjectNode(expression)
            override fun visitVararg(expression: IrVararg, data: BuilderState) = data.constObjectNode(expression)

            override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: BuilderState) = Node.Unit

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BuilderState) = checkGraphSizeAndVisit(expression, data) {
                val argResult = expression.argument.accept(this, data)
                when (expression.operator) {
                    IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.SAFE_CAST -> argResult
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> Node.Unit
                    IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> data.constObjectNode(expression)
                    else -> error("Not expected: ${expression.operator}")
                }
            }

            override fun visitGetValue(expression: IrGetValue, data: BuilderState) = checkGraphSizeAndVisit(expression, data) {
                when (val owner = expression.symbol.owner) {
                    is IrValueParameter -> data.graph.parameterNodes[owner] ?: error("Unknown value parameter: ${owner.render()}")
                    is IrVariable -> data.graph.variableNodes[owner] ?: error("Unknown variable: ${owner.render()}")
                    else -> error("Unknown value declaration: ${owner.render()}")
                }
            }

            fun PointsToGraph.eraseValue(reference: Node.Reference, nodeContext: NodeContext, anchorNodeId: Int = 0) {
                if (reference.assignedTo.isNotEmpty())
                    bypass(reference, nodeContext, anchorNodeId)
                else
                    reference.assignedWith.clear()
            }

            override fun visitSetValue(expression: IrSetValue, data: BuilderState) = data.graph.checkSizeAndVisit(expression) {
                debug {
                    context.log { "before ${expression.dump()}" }
                    logDigraph()
                }

                val valueNode = expression.value.accept(this@PointsToGraphBuilder, data)
                debug {
                    context.log { "after evaluating value" }
                    logDigraph(valueNode)
                }

                val variable = expression.symbol.owner as IrVariable
                val variableNode = variableNodes[variable] ?: error("Unknown variable: ${variable.render()}")
                if (valueNode == Node.Nothing)
                    unreachable()
                else {
                    if (data.tryBlock == null)
                        eraseValue(variableNode, data.toNodeContext(expression))
                    variableNode.addEdge(valueNode)

                    Node.Unit
                }.also {
                    debug {
                        context.log { "after ${expression.dump()}" }
                        logDigraph(variableNode)
                    }
                }
            }

            override fun visitVariable(declaration: IrVariable, data: BuilderState) = data.graph.checkSizeAndVisit(declaration) {
                debug {
                    context.log { "before ${declaration.dump()}" }
                    logDigraph()
                }

                require(data.loop != null || variableNodes[declaration] == null) {
                    "Duplicate variable declaration: ${declaration.render()}"
                }

                val initializer = declaration.initializer
                val valueNode = initializer?.accept(this@PointsToGraphBuilder, data)
                debug {
                    valueNode?.let {
                        context.log { "after evaluating initializer" }
                        logDigraph(it)
                    }
                }

                val variableNode = getOrAddVariable(declaration, data.level)
                if (valueNode == Node.Nothing)
                    unreachable()
                else {
                    valueNode?.let {
                        val fictitiousSetValue = fictitiousVariableInitSetValues.getOrPut(declaration) {
                            irBuilder.irSetVar(declaration, initializer)
                        }
                        if (data.tryBlock == null)
                            eraseValue(variableNode, data.toNodeContext(fictitiousSetValue))
                        variableNode.addEdge(it)
                    }

                    Node.Unit
                }.also {
                    debug {
                        context.log { "after ${declaration.dump()}" }
                        logDigraph(variableNode)
                    }
                }
            }

            override fun visitGetField(expression: IrGetField, data: BuilderState) = data.graph.checkSizeAndVisit(expression) {
                debug {
                    context.log { "before ${expression.dump()}" }
                    logDigraph()
                }

                val receiverNode = expression.receiver?.accept(this@PointsToGraphBuilder, data) ?: globalNode
                debug {
                    context.log { "after evaluating receiver" }
                    logDigraph(receiverNode)
                }

                val receiverObjects =
                        if (data.graph.forest.totalNodes > maxAllowedGraphSize)
                            emptyList()
                        else
                            getObjectNodes(receiverNode, data.toNodeContext(expression), 1)
                debug {
                    context.log { "after getting receiver's objects" }
                    logDigraph(context, receiverObjects)
                }

                return (when (receiverObjects.size) {
                    0 -> { // This is either unreachable or will lead to a segfault at runtime.
                        unreachable()
                    }
                    1 -> {
                        receiverObjects[0].getField(expression.symbol)
                    }
                    else -> at(data.toNodeContext(expression)).newPhiNode().also { phiNode ->
                        for (receiver in receiverObjects)
                            phiNode.addEdge(receiver.getField(expression.symbol))
                    }
                }).also {
                    debug {
                        context.log { "after ${expression.dump()}" }
                        logDigraph(it)
                    }
                }
            }

            override fun visitSetField(expression: IrSetField, data: BuilderState) = data.graph.checkSizeAndVisit(expression) {
                debug {
                    context.log { "before ${expression.dump()}" }
                    logDigraph()
                }

                val receiverNode = expression.receiver?.accept(this@PointsToGraphBuilder, data) ?: globalNode
                debug {
                    context.log { "after evaluating receiver" }
                    logDigraph(receiverNode)
                }

                val valueNode = expression.value.accept(this@PointsToGraphBuilder, data)
                debug {
                    context.log { "after evaluating value" }
                    logDigraph(valueNode)
                }

                if (valueNode == Node.Nothing)
                    unreachable()
                else {
                    val receiverObjects =
                            if (data.graph.forest.totalNodes > maxAllowedGraphSize)
                                emptyList()
                            else
                                getObjectNodes(data.graph.nodes[receiverNode.id]!!, data.toNodeContext(expression), 1)
                    debug {
                        context.log { "after getting receiver's objects" }
                        logDigraph(context, receiverObjects)
                    }

                    if (receiverObjects.isEmpty()) {
                        // This is either unreachable or will lead to a segfault at runtime.
                        unreachable()
                    } else {
                        receiverObjects.forEach { receiverObject ->
                            val fieldNode = receiverObject.getField(expression.symbol)
                            // TODO: Probably can do for any outer loop as well (not only for the current).
                            if (receiverObjects.size == 1 // Otherwise, we don't know which object actually gets its field rewritten.
                                    // 'intestines' is not a single field but rather all internals of an object,
                                    // we don't know which actual field gets rewritten.
                                    && expression.symbol != intestinesField
                                    && data.loop == receiverObjects[0].loop && data.tryBlock == null
                            ) {
                                eraseValue(fieldNode, data.toNodeContext(expression))
                            }
                            fieldNode.addEdge(valueNode)
                        }

                        Node.Unit
                    }
                }.also {
                    debug {
                        context.log { "after ${expression.dump()}" }
                        logDigraph()
                    }
                }
            }

            override fun visitReturn(expression: IrReturn, data: BuilderState) = checkGraphSizeAndVisit(expression, data) {
                val result = expression.value.accept(this, data)
                val returnResult = returnTargetResults[expression.returnTargetSymbol]
                        ?: error("Unknown return target: ${expression.render()}")
                if (result != Node.Nothing) {
                    debug {
                        context.log { "Merging to return of ${expression.returnTargetSymbol.owner.render()}" }
                        data.graph.logDigraph(result)
                    }

                    returnResult.merge(result, data.graph)

                    debug {
                        context.log { "After merging:" }
                        returnResult.graphBuilder.build().logDigraph(context) { markedNodes.or(returnResult.valueIds) }
                    }
                }
                data.graph.unreachable()
            }

            override fun visitContainerExpression(expression: IrContainerExpression, data: BuilderState) = checkGraphSizeAndVisit(expression, data) {
                val returnableBlockSymbol = (expression as? IrReturnableBlock)?.symbol
                if (returnableBlockSymbol == null) {
                    expression.statements.forEachIndexed { index, statement ->
                        val statementNode = statement.accept(this, data)
                        if (statementNode == Node.Nothing)
                            return@checkGraphSizeAndVisit data.graph.unreachable()
                        if (index == expression.statements.size - 1)
                            return@checkGraphSizeAndVisit statementNode
                    }
                    Node.Unit
                } else {
                    val returnableBlockResult = MultipleExpressionResult(BitSet(), forest.GraphBuilder(function))
                    returnTargetResults[returnableBlockSymbol] = returnableBlockResult
                    for (statement in expression.statements) {
                        val statementNode = statement.accept(this, data)
                        if (statementNode == Node.Nothing) break
                    }
                    controlFlowMergePoint(data.graph, data.toNodeContext(expression), expression.type, returnableBlockResult)
                }
            }

            override fun visitWhen(expression: IrWhen, data: BuilderState) = checkGraphSizeAndVisit(expression, data) {
                debug {
                    context.log { "before ${expression.dump()}" }
                    data.graph.logDigraph()
                }
                val branchResults = mutableListOf<ExpressionResult>()
                for (branch in expression.branches) {
                    val conditionNode = branch.condition.accept(this, data)
                    if (conditionNode == Node.Nothing) break
                    val branchGraph = data.graph.clone()
                    val branchState = BuilderState(branchGraph, data.level, data.anchorIds, data.loop, data.tryBlock)
                    branchResults.add(ExpressionResult(branch.result.accept(this, branchState), branchGraph))
                }
                val isExhaustive = expression.branches.last().isUnconditional()
                require(isExhaustive || expression.type.isUnit())
                if (!isExhaustive) {
                    // Reflecting the case when none of the clauses have been executed.
                    branchResults.add(ExpressionResult(Node.Unit, data.graph.clone()))
                }
                controlFlowMergePoint(data.graph, data.toNodeContext(expression), expression.type, branchResults).also {
                    debug {
                        context.log { "after ${expression.dump()}" }
                        data.graph.logDigraph()
                    }
                }
            }

            override fun visitThrow(expression: IrThrow, data: BuilderState) = data.graph.checkSizeAndVisit(expression) {
                debug {
                    context.log { "before ${expression.dump()}" }
                    data.graph.logDigraph()
                }
                val value = expression.value.accept(this@PointsToGraphBuilder, data)
                val visited = BitSet()
                val newObjectsReferencedFromThrown = mutableListOf<Node.Object>()

                fun markReachableObjects(node: Node) {
                    visited.set(node.id)
                    if (node is Node.Object && !objectsReferencedFromThrown.get(node.id)) {
                        objectsReferencedFromThrown.set(node.id)
                        newObjectsReferencedFromThrown.add(node)
                    }
                    node.forEachPointee {
                        if (!visited.get(it.id)) markReachableObjects(it)
                    }
                }

                markReachableObjects(value)
                debug {
                    context.log { "after ${expression.dump()}" }
                    data.graph.logDigraph(context, newObjectsReferencedFromThrown)
                }

                (data.tryBlock?.let { tryBlocksThrowGraphs[it]!! } ?: returnTargetResults[function.symbol]!!.graphBuilder)
                        .merge(data.graph)

                data.graph.unreachable()
            }

            override fun visitCatch(aCatch: IrCatch, data: BuilderState) = checkGraphSizeAndVisit(aCatch, data) {
                require(aCatch.catchParameter.initializer == null) {
                    "Non-null initializer of a catch parameter: ${aCatch.catchParameter.render()}"
                }
                data.graph.getOrAddVariable(aCatch.catchParameter, data.level)
                aCatch.result.accept(this, data)
            }

            /*
              TODO | A more precise way of handling try/catch/throw is to find all the points in the program
              TODO | which can throw an exception (an explicit throw, an external function call, an instance method call,
              TODO | or an instance field read/write), then connect all these points to the catch clauses.
              TODO | We would probably need to save two graphs per function instead of one: usual return and failure.

              TODO | A note: this probably can lead to automatic nounwind attribute evaluation as a side effect
              TODO | (as long as all runtime functions are marked with annotation/attribute).

              TODO | And even more precise way if we distinguish between different exception types.
              TODO | (And save the graphs for all types as well). But it looks overkillish to me.
             */
            override fun visitTry(aTry: IrTry, data: BuilderState) = checkGraphSizeAndVisit(aTry, data) {
                require(aTry.finallyExpression == null) { "All finally clauses should've been lowered out" }
                debug {
                    context.log { "before ${aTry.dump()}" }
                    data.graph.logDigraph()
                }

                val tryBlockThrowGraph = forest.GraphBuilder(function)
                tryBlocksThrowGraphs[aTry] = tryBlockThrowGraph
                val tryGraph = data.graph.clone()
                val tryResult = aTry.tryResult.accept(this, BuilderState(tryGraph, data.level, true, data.loop, aTry))
                val tryGraphWithThrows = if (tryBlockThrowGraph.isEmpty)
                    tryGraph
                else {
                    tryBlockThrowGraph.merge(tryGraph)
                    tryBlockThrowGraph.build()
                }
                val catchesResults = aTry.catches.map {
                    val catchGraph = tryGraphWithThrows.clone()
                    val catchResult = it.accept(this, BuilderState(catchGraph, data.level, data.anchorIds, data.loop, data.tryBlock))
                    ExpressionResult(catchResult, catchGraph)
                }

                /* TODO: Optimize this case.
                   b.a = A()
                   try {
                       if (f) throw ..
                       b.a = A()
                   } catch (..) { b.a = a }
                   Here after the try block b.a gets rewritten but now we conservatively assume all the possible values,
                   because the catch clause result isn't Nothing.
                 */
                if (catchesResults.all { it.value == Node.Nothing }) {
                    // We can get here only if no exception has been thrown at the try block
                    // (otherwise, it either would've been caught by one of the catch blocks or
                    // would've been thrown to upper scope and, since they all return nothing,
                    // the control flow wouldn't have gotten to this point).
                    aTry.tryResult.accept(this, BuilderState(data.graph, data.level, true, data.loop, null))
                } else {
                    controlFlowMergePoint(data.graph, data.toNodeContext(aTry), aTry.type,
                            listOf(ExpressionResult(tryResult, tryGraph)) + catchesResults)
                }.also {
                    debug {
                        context.log { "after ${aTry.dump()}" }
                        data.graph.logDigraph(it)
                    }
                }
            }

            override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: BuilderState) = checkGraphSizeAndVisit(expression, data) {
                expression.suspensionPointIdParameter.accept(this, data)
                val normalResultGraph = data.graph.clone()
                val normalResult = expression.result.accept(this, BuilderState(normalResultGraph, data.level, data.anchorIds, data.loop, data.tryBlock))
                val resumeResultGraph = data.graph.clone()
                val resumeResult = expression.resumeResult.accept(this, BuilderState(resumeResultGraph, data.level, data.anchorIds, data.loop, data.tryBlock))
                controlFlowMergePoint(data.graph, data.toNodeContext(expression), expression.type,
                        listOf(ExpressionResult(normalResult, normalResultGraph), ExpressionResult(resumeResult, resumeResultGraph))
                )
            }

            override fun visitSuspendableExpression(expression: IrSuspendableExpression, data: BuilderState) =
                    expression.result.accept(this, data)

            override fun visitContinue(jump: IrContinue, data: BuilderState): Node {
                (loopsContinueGraphs[jump.loop] ?: error("A continue from an unknown loop: ${jump.loop}"))
                        .merge(data.graph)
                return data.graph.unreachable()
            }

            override fun visitBreak(jump: IrBreak, data: BuilderState): Node {
                (loopsBreakGraphs[jump.loop] ?: error("A break from an unknown loop: ${jump.loop}"))
                        .merge(data.graph)
                return data.graph.unreachable()
            }

            override fun visitLoop(loop: IrLoop, data: BuilderState) = checkGraphSizeAndVisit(loop, data) {
                debug {
                    context.log { "before ${loop.dump()}" }
                    data.graph.logDigraph()
                }

                var graph = data.graph
                val loopGraph = forest.GraphBuilder(function)
                if (loop is IrWhileLoop) {
                    loop.condition.accept(this, BuilderState(graph, data.level + 1, true, loop, data.tryBlock))
                    // A while loop might not execute even a single iteration.
                    loopGraph.merge(graph)
                }
                loopsBreakGraphs[loop] = loopGraph

                var iteration = 0
                do {
                    debug {
                        context.log { "iter#$iteration" }
                        context.log { "current graph:" }
                        graph.logDigraph()
                        context.log { "accumulated graph:" }
                        loopGraph.build().logDigraph()
                    }
                    ++iteration

                    val continueGraphBuilder = forest.GraphBuilder(function)
                    loopsContinueGraphs[loop] = continueGraphBuilder
                    val modCountsSum = 0 +
                            returnTargetResults.values.sumOf {
                                it.valueIds.cardinality() + it.graphBuilder.modCount
                            } +
                            tryBlocksThrowGraphs.values.sumOf { it.modCount } +
                            loopsContinueGraphs.values.sumOf { it.modCount } +
                            loopsBreakGraphs.values.sumOf { it.modCount } +
                            objectsReferencedFromThrown.cardinality()

                    loop.body?.accept(this, BuilderState(graph, data.level + 1, true, loop, data.tryBlock))
                    continueGraphBuilder.merge(graph)
                    graph = continueGraphBuilder.build()
                    loop.condition.accept(this, BuilderState(graph, data.level + 1, true, loop, data.tryBlock))
                    loopGraph.merge(graph)
                    loopsContinueGraphs.remove(loop)

                    val nextModCountsSum = 0 +
                            returnTargetResults.values.sumOf {
                                it.valueIds.cardinality() + it.graphBuilder.modCount
                            } +
                            tryBlocksThrowGraphs.values.sumOf { it.modCount } +
                            loopsContinueGraphs.values.sumOf { it.modCount } +
                            loopsBreakGraphs.values.sumOf { it.modCount } +
                            objectsReferencedFromThrown.cardinality()
                } while (modCountsSum != nextModCountsSum && iteration < 10)

                if (iteration >= 10)
                    error("BUGBUGBUG: ${function.render()} ${loop.dump()}")
                data.graph.copyFrom(loopGraph.build())
                debug {
                    context.log { "after ${loop.dump()}" }
                    data.graph.logDigraph()
                }

                Node.Unit
            }

            fun processCall(
                    state: BuilderState,
                    callSite: IrFunctionAccessExpression,
                    callee: IrFunction,
                    argumentNodeIds: List<Int>,
                    calleeEscapeAnalysisResult: EscapeAnalysisResult,
            ): Node {
                if (state.graph.forest.totalNodes > maxAllowedGraphSize) {
                    context.log { "The graph is bigger than expected - skipping call to ${callee.render()}" }
                    return state.graph.unreachable()
                }

                debug {
                    context.log { "before calling ${callee.render()}" }
                    state.graph.logDigraph()
                    context.log { "callee EA result" }
                    calleeEscapeAnalysisResult.graph.logDigraph(calleeEscapeAnalysisResult.returnValue)
                    context.log { "argumentIds: ${argumentNodeIds.joinToString { it.toString() }}" }
                }

                val arguments = argumentNodeIds.map { state.graph.nodes[it]!! }

                debug {
                    context.log { "arguments: ${arguments.joinToString { it.toString() }}" }
                }

                val calleeGraph = calleeEscapeAnalysisResult.graph
                require(arguments.size == calleeGraph.parameterNodes.size)

                val referencesCount = IntArray(calleeGraph.nodes.size)
                for (node in calleeGraph.nodes) {
                    if (node !is Node.Reference) continue
                    for (pointee in node.assignedWith) {
                        if (pointee is Node.Object) ++referencesCount[pointee.id]
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

                    debug {
                        context.log { "Reflecting $node to ($inMirroredNode, $outMirroredNode)" }
                    }

                    inMirroredNodes[node.id] = inMirroredNode
                    outMirroredNodes[node.id] = outMirroredNode
                }

                fun reflectFieldsOf(fictitiousObject: Node.Object, actualObjects: List<Node.Object>) {
                    class PossiblySplitMirroredNode {
                        var inNode: Node.Variable? = null
                        var outNode: Node.Variable? = null

                        fun reflect(node: Node) {
                            require(inNode != null || outNode != null) { "Cannot reflect $node" }
                            reflectNode(node, inNode ?: outNode!!, outNode ?: inNode!!)
                        }
                    }

                    if (calleeEscapeAnalysisResult.objectsReferencedFromThrown.get(fictitiousObject.id))
                        actualObjects.forEach { objectsReferencedFromThrown.set(it.id) }

                    fictitiousObject.fields.forEach { (field, fieldValue) ->
                        val fieldPointee = fieldValue.assignedWith.atMostOne()
                        if (fieldPointee == null && fieldValue.assignedTo.isEmpty())
                            require(fieldValue == calleeEscapeAnalysisResult.returnValue) {
                                "The node $fieldValue should've been optimized away"
                            }

                        val hasIncomingEdges = fieldValue.assignedTo.isNotEmpty() || fieldValue == calleeEscapeAnalysisResult.returnValue
                        val canOmitFictitiousObject = fieldPointee is Node.Object
                                && fieldPointee.isFictitious
                                && inMirroredNodes[fieldPointee.id] == null // Skip cycles.

                        val mirroredNode = when {
                            actualObjects.size == 1 && actualObjects[0].loop == state.loop && state.tryBlock == null -> {
                                // Can reflect directly field value to field value.
                                null
                            }
                            actualObjects.isEmpty() -> { // This is going to be a segfault at runtime.
                                reflectNode(fieldValue, Node.Null)
                                null
                            }
                            else -> PossiblySplitMirroredNode().also {
                                if (hasIncomingEdges || fieldPointee == null)
                                    it.inNode = state.graph.at(state.toNodeContext(callSite), fieldValue.id).newPhiNode()
                                if (fieldPointee != null && !canOmitFictitiousObject)
                                    it.outNode = state.graph.at(state.toNodeContext(callSite), fieldValue.id + calleeGraph.nodes.size).newTempVariable()
                            }.takeIf { it.inNode != null || it.outNode != null }
                        }
                        mirroredNode?.reflect(fieldValue)

                        if (canOmitFictitiousObject) {
                            fieldPointee as Node.Object
                            val nextActualObjects = mutableListOf<Node.Object>()
                            for (obj in actualObjects) {
                                val objFieldValue = with(state.graph) { obj.getField(field) }
                                if (hasIncomingEdges) {
                                    if (mirroredNode == null)
                                        reflectNode(fieldValue, objFieldValue)
                                    else {
                                        require(mirroredNode.outNode == null)
                                        mirroredNode.inNode!!.addEdge(objFieldValue)
                                    }
                                }
                                nextActualObjects.addAll(
                                        state.graph.getObjectNodes(objFieldValue,
                                                NodeContext(state.level, state.anchorIds, fieldPointee.loop ?: state.loop, callSite),
                                                fieldValue.id + calleeGraph.nodes.size
                                        )
                                )
                            }
                            handledNodes.set(fieldValue.id)

                            // TODO: What if have to do this for more than one callsite (which loop to take?)
                            fieldPointee.loop?.let {
                                nextActualObjects.forEach { obj -> obj.loop = it }
                            }
                            if (referencesCount[fieldPointee.id] > 1) {
                                when (nextActualObjects.size) {
                                    0 -> reflectNode(fieldPointee, Node.Null)
                                    1 -> reflectNode(fieldPointee, nextActualObjects[0])
                                    else -> {
                                        val mirroredFieldPointee = state.graph.at(state.toNodeContext(callSite), fieldPointee.id).newPhiNode()
                                        mirroredFieldPointee.assignedWith.addAll(nextActualObjects)
                                        reflectNode(fieldPointee, mirroredFieldPointee)
                                    }
                                }
                            }
                            reflectFieldsOf(fieldPointee, nextActualObjects)
                        } else {
                            for (obj in actualObjects) {
                                val objFieldValue = with(state.graph) { obj.getField(field) }
                                if (fieldPointee != null) { // An actual field rewrite.
                                    // TODO: Probably can do for any outer loop as well (not only for the current).
                                    // Same conditions as for IrSetField (see the comment there).
                                    if (actualObjects.size == 1
                                            && field != intestinesField
                                            && state.loop == obj.loop && state.tryBlock == null
                                    ) {
                                        state.graph.eraseValue(objFieldValue, state.toNodeContext(callSite), fieldValue.id + 2 * calleeGraph.nodes.size)
                                    } else {
                                        require(mirroredNode?.outNode != null || field == intestinesField)
                                    }
                                }

                                if (mirroredNode == null)
                                    reflectNode(fieldValue, objFieldValue)
                                else {
                                    mirroredNode.inNode?.addEdge(objFieldValue)
                                    mirroredNode.outNode?.let { objFieldValue.addEdge(it) }
                                }
                            }
                        }
                    }
                    handledNodes.set(fictitiousObject.id)
                }

                reflectNode(Node.Nothing, Node.Nothing)
                reflectNode(Node.Null, Node.Null)
                reflectNode(Node.Unit, Node.Unit)
                reflectNode(calleeEscapeAnalysisResult.graph.globalNode, state.graph.globalNode)
                calleeEscapeAnalysisResult.graph.globalNode.fields.forEach { (field, fieldValue) ->
                    reflectNode(fieldValue, with(state.graph) { globalNode.getField(field) })
                }
                for (parameter in calleeEscapeAnalysisResult.graph.parameterNodes.values)
                    reflectNode(parameter, arguments[parameter.index])
                for (parameter in calleeEscapeAnalysisResult.graph.parameterNodes.values)
                    reflectFieldsOf(parameter, state.graph.getObjectNodes(arguments[parameter.index], state.toNodeContext(callSite), parameter.id))

                for (node in calleeGraph.nodes) {
                    if (handledNodes.get(node!!.id) || inMirroredNodes[node.id] != null) continue
                    when (node) {
                        is Node.Parameter -> error("Parameter $node should've been handled earlier")
                        is Node.Variable -> reflectNode(node, state.graph.at(state.toNodeContext(callSite), node.id).newTempVariable(node.label))
                        is Node.FieldValue -> Unit // Will be mirrored along with its owner object.
                        is Node.Object -> {
                            val mirroredObject = state.graph.at(
                                    NodeContext(state.level, state.anchorIds, node.loop ?: state.loop, callSite), node.id
                            ).newObject(node.label)
                            reflectNode(node, mirroredObject)
                            // TODO: Do we need to do this?
                            if (calleeEscapeAnalysisResult.objectsReferencedFromThrown.get(node.id))
                                objectsReferencedFromThrown.set(mirroredObject.id)
                            node.fields.forEach { (field, fieldValue) ->
                                reflectNode(fieldValue, with(state.graph) { mirroredObject.getField(field) })
                            }
                        }
                    }
                }

                for (node in calleeGraph.nodes) {
                    if (handledNodes.get(node!!.id) || inMirroredNodes[node.id] != null) continue
                    require(node is Node.FieldValue) { "The node $node should've been reflected at this point" }
                    reflectNode(node, state.graph.at(state.toNodeContext(callSite), node.id).newTempVariable())
                }

                for (node in calleeGraph.nodes) {
                    if (handledNodes.get(node!!.id) || node !is Node.Reference) continue

                    val outMirroredNode = outMirroredNodes[node.id]
                    if (outMirroredNode == Node.Null) continue
                    require(outMirroredNode is Node.Reference)
                    for (pointee in node.assignedWith) {
                        val inMirroredNode = inMirroredNodes[pointee.id]
                        if (inMirroredNode == Node.Null && pointee != Node.Null) continue
                        outMirroredNode.addEdge(inMirroredNode ?: error("Node $pointee hasn't been reflected"))
                    }
                }

                val returnValue = outMirroredNodes[calleeEscapeAnalysisResult.returnValue.id]
                        ?: error("Node ${calleeEscapeAnalysisResult.returnValue} hasn't been reflected")

                debug {
                    context.log { "after calling ${callee.render()}" }
                    state.graph.logDigraph(returnValue)
                }

                if (returnValue != Node.Nothing)
                    return returnValue

                // The callee always throws an exception.
                // TODO: This is not always true - the function call might not return (an infinite loop inside).
                (state.tryBlock?.let { tryBlocksThrowGraphs[it]!! } ?: returnTargetResults[function.symbol]!!.graphBuilder)
                        .merge(state.graph)
                return state.graph.unreachable()
            }

            // TODO: Move to KonanFqNames or something.
            private val NAME_ESCAPES = Name.identifier("Escapes")
            private val NAME_POINTS_TO = Name.identifier("PointsTo")
            private val FQ_NAME_KONAN = FqName.fromSegments(listOf("kotlin", "native", "internal"))

            private val FQ_NAME_ESCAPES = FQ_NAME_KONAN.child(NAME_ESCAPES)
            private val FQ_NAME_POINTS_TO = FQ_NAME_KONAN.child(NAME_POINTS_TO)

            fun getExternalFunctionEAResult(function: IrFunction): EscapeAnalysisResult {
                context.log { "An external call: ${function.render()}" }
                // TODO: use package instead.
                val fqName = function.fqNameForIrSerialization.asString()
                return if (function.isStaticInitializer) // TODO: Is this correct?
                    EscapeAnalysisResult.optimistic(function)
                else if (fqName.startsWith("kotlin.")
                        // TODO: Is it possible to do it in a more fine-grained fashion?
                        && !fqName.startsWith("kotlin.native.concurrent")) {
                    context.log { "A function from K/N runtime - can use annotations" }
                    val escapesAnnotation = function.annotations.findAnnotation(FQ_NAME_ESCAPES)
                    val pointsToAnnotation = function.annotations.findAnnotation(FQ_NAME_POINTS_TO)
                    @Suppress("UNCHECKED_CAST")
                    val escapesBitMask = (escapesAnnotation?.getValueArgument(0) as? IrConst<Int>)?.value
                    @Suppress("UNCHECKED_CAST")
                    val pointsToBitMask = (pointsToAnnotation?.getValueArgument(0) as? IrVararg)?.elements?.map { (it as IrConst<Int>).value }
                    EscapeAnalysisResult.fromBits(
                            function,
                            escapesBitMask ?: 0,
                            (0..function.allParameters.size).map { pointsToBitMask?.elementAtOrNull(it) ?: 0 }
                    )
                } else {
                    context.log { "An unknown function - assume pessimistic result" }
                    EscapeAnalysisResult.pessimistic(function)
                }
            }

            fun processConstructorCall(
                    callee: IrConstructor,
                    thisNode: Node,
                    callSite: IrFunctionAccessExpression,
                    state: BuilderState
            ): Node {
                val arguments = callSite.getArgumentsWithIr()
                val argumentNodeIds = mutableListOf(thisNode.id)
                for (argument in arguments) {
                    val argumentNode = argument.second.accept(this, state)
                    argumentNodeIds.add(argumentNode.id)
                    if (argumentNode == Node.Nothing) break
                }
                if (argumentNodeIds.any { it == Node.NOTHING_ID }
                        || argumentNodeIds.any { it >= state.graph.nodes.size || state.graph.nodes[it] == null }) {
                    context.log { "Unreachable code - skipping call to ${callee.render()}" }
                    return state.graph.unreachable()
                }

                val calleeEscapeAnalysisResult = escapeAnalysisResults[callee.symbol]
                        ?: getExternalFunctionEAResult(callee)
                processCall(state, callSite, callee, argumentNodeIds, calleeEscapeAnalysisResult)
                return Node.Unit
            }

            fun BuilderState.allocObjectNode(expression: IrFunctionAccessExpression, irClass: IrClass) =
                    graph.at(toNodeContext(expression)).newObject("<A:${irClass.name}>").also {
                        allocations[expression] = it.id
                    }

            override fun visitConstructorCall(expression: IrConstructorCall, data: BuilderState) = checkGraphSizeAndVisit(expression, data) {
                val thisObject = data.allocObjectNode(expression, expression.symbol.owner.constructedClass)
                val callResult = processConstructorCall(expression.symbol.owner, thisObject, expression, data)
                if (callResult == Node.Nothing || data.graph.forest.totalNodes > maxAllowedGraphSize
                        || thisObject.id >= data.graph.nodes.size || data.graph.nodes[thisObject.id] == null)
                    data.graph.unreachable()
                else
                    data.graph.nodes[thisObject.id]!!
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: BuilderState) = checkGraphSizeAndVisit(expression, data) {
                val constructor = expression.symbol.owner
                val thisReceiver = (function as IrConstructor).constructedClass.thisReceiver!!
                val irThis = IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, thisReceiver.type, thisReceiver.symbol)
                return processConstructorCall(constructor, irThis.accept(this, data), expression, data)
            }

            val createUninitializedInstanceSymbol = context.ir.symbols.createUninitializedInstance
            val initInstanceSymbol = context.ir.symbols.initInstance
            val reinterpretSymbol = context.ir.symbols.reinterpret
            val executeImplSymbol = context.ir.symbols.executeImpl

            // TODO: What about staticInitializer, executeImpl, getContinuation?
            override fun visitCall(expression: IrCall, data: BuilderState) = when (expression.symbol) {
                createUninitializedInstanceSymbol -> {
                    data.allocObjectNode(expression, expression.getTypeArgument(0)!!.classOrNull!!.owner)
                }
                initInstanceSymbol -> {
                    val irThis = expression.getValueArgument(0)!!
                    val thisNode = irThis.accept(this, data)
                    val irInitializer = expression.getValueArgument(1) as IrConstructorCall
                    processConstructorCall(irInitializer.symbol.owner, thisNode, irInitializer, data)
                }
                reinterpretSymbol -> {
                    expression.extensionReceiver!!.accept(this, data)
                }
//                executeImplSymbol -> {
//                    TODO()
//                }
                else -> {
                    val actualCallee = expression.actualCallee
                    val arguments = expression.getArgumentsWithIr()
                    val argumentNodeIds = mutableListOf<Int>()
                    for (argument in arguments) {
                        val argumentNode = argument.second.accept(this, data)
                        argumentNodeIds.add(argumentNode.id)
                        if (argumentNode == Node.Nothing)
                            break
                    }
                    if (argumentNodeIds.any { it == Node.NOTHING_ID }
                            || argumentNodeIds.any { it >= data.graph.nodes.size || data.graph.nodes[it] == null }) {
                        context.log { "Unreachable code - skipping call to ${actualCallee.render()}" }
                        data.graph.unreachable()
                    } else {
                        if (!expression.isVirtualCall) {
                            val calleeEscapeAnalysisResult = escapeAnalysisResults[actualCallee.symbol]
                                    ?: getExternalFunctionEAResult(actualCallee)
                            processCall(data, expression, actualCallee, argumentNodeIds, calleeEscapeAnalysisResult)
                        } else {
                            val devirtualizedCallSite = devirtualizedCallSites[expression]
                            if (devirtualizedCallSite == null) {
                                // Non-devirtualized call.
                                processCall(data, expression, expression.actualCallee, argumentNodeIds,
                                        EscapeAnalysisResult.pessimistic(actualCallee))
                            } else when (devirtualizedCallSite.size) {
                                0 -> {
                                    // No actual callees - this call site bound to be unreachable.
                                    data.graph.unreachable()
                                }
                                1 -> {
                                    processCall(data, expression, devirtualizedCallSite[0].owner, argumentNodeIds,
                                            escapeAnalysisResults[devirtualizedCallSite[0]]
                                                    ?: getExternalFunctionEAResult(devirtualizedCallSite[0].owner))
                                }
                                else -> {
                                    // Multiple possible callees - model this as a when clause.
                                    val fictitiousCallSites = devirtualizedFictitiousCallSites.getOrPut(expression) {
                                        // Don't bother with the arguments - it is only used as a key.
                                        devirtualizedCallSite.map { irBuilder.irCall(it) }
                                    }
                                    val clonedGraphs = devirtualizedCallSite.indices.map { data.graph.clone() }
                                    val callResults = devirtualizedCallSite.mapIndexed { index, calleeSymbol ->
                                        val callSite = fictitiousCallSites[index]
                                        val clonedGraph = clonedGraphs[index]
                                        val callee = calleeSymbol.owner
                                        val resultNode = processCall(
                                                BuilderState(clonedGraph, data.level, data.anchorIds, data.loop, data.tryBlock),
                                                callSite, callee,
                                                argumentNodeIds,
                                                escapeAnalysisResults[calleeSymbol] ?: getExternalFunctionEAResult(callee))
                                        ExpressionResult(resultNode, clonedGraph)
                                    }
                                    controlFlowMergePoint(data.graph, data.toNodeContext(expression), expression.type, callResults)
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun intraproceduralAnalysis(
                callGraphNode: CallGraphNode,
                escapeAnalysisResults: MutableMap<IrFunctionSymbol, EscapeAnalysisResult>,
                lifetimes: MutableMap<IrFunctionAccessExpression, Lifetime>,
                allocationToFunction: MutableMap<IrFunctionAccessExpression, IrFunction>,
                maxAllowedGraphSize: Int,
        ): Boolean {
            val function = callGraphNode.symbol.irFunction!!
            if (function.body == null) return true

            // TODO: Use these maps or remove.
            val producerInvocations = mutableMapOf<IrExpression, IrCall>()
            val jobInvocations = mutableMapOf<IrCall, IrCall>()
            val devirtualizedCallSites = mutableMapOf<IrCall, MutableList<IrFunctionSymbol>>()
            val failedToDevirtualizeCallSites = mutableSetOf<IrCall>()
            for (callSite in callGraphNode.callSites) {
                val call = callSite.call
                val irCall = call.irCallSite as? IrCall ?: continue
                if (irCall.origin == STATEMENT_ORIGIN_PRODUCER_INVOCATION)
                    producerInvocations[irCall.dispatchReceiver!!] = irCall
                else if (irCall.origin == STATEMENT_ORIGIN_JOB_INVOCATION)
                    jobInvocations[irCall.getValueArgument(0) as IrCall] = irCall
                if (call !is DataFlowIR.Node.VirtualCall) continue
                if (callSite.isVirtual)
                    failedToDevirtualizeCallSites.add(irCall)
                devirtualizedCallSites.getOrPut(irCall) { mutableListOf() }.add(
                        callSite.actualCallee.irFunction?.symbol ?: error("No IR for ${callSite.actualCallee}")
                )
            }
            // TODO: Remove after testing.
            failedToDevirtualizeCallSites.forEach {
                val list = devirtualizedCallSites[it] ?: return@forEach
                require(list.size == 1)
                devirtualizedCallSites.remove(it)
            }

            val forest = PointsToGraphForest(function)
            val (functionResult, allocations) = PointsToGraphBuilder(
                    function, forest, escapeAnalysisResults, devirtualizedCallSites,
                    maxAllowedGraphSize, needDebug(function)
            ).build()

            if (forest.totalNodes > maxAllowedGraphSize) return false

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
            functionResult.logDigraph(context)

            functionResult.computeLifetimes(allocations, lifetimes)
            allocations.keys.forEach { allocationToFunction[it] = function }
            context.log { "lifetimes computation results:" }
            functionResult.graph.log(context)
            functionResult.graph.logDigraph(context) {
                highlightLifetimes = true
                +functionResult.returnValue
            }

            functionResult.graph.variableNodes.clear() // No need in this map anymore.
            functionResult.removeUnreachable()
            context.log { "after removing unreachable:" }
            functionResult.logDigraph(context)

            functionResult.removeRedundantNodesPart1()
            context.log { "after removing redundant nodes (part 1):" }
            functionResult.logDigraph(context)

            // TODO: Remove phi nodes with either one incoming or one outgoing edge.
            functionResult.bypassAndRemoveVariables()
            context.log { "after bypassing variables:" }
            functionResult.logDigraph(context)

            // Remove multi-edges.
            functionResult.reenumerateNodes()
            val list = mutableListOf(functionResult.returnValue)
            val graph = PointsToGraphForest(functionResult.graph.nodes.size).mergeGraphs(listOf(functionResult.graph), list)
            val returnValue = list[0]
            val optimizedFunctionResult = EscapeAnalysisResult(graph, returnValue, functionResult.objectsReferencedFromThrown)
            context.log { "after removing multi-edges:" }
            optimizedFunctionResult.logDigraph(context)

            optimizedFunctionResult.removeRedundantNodesPart2()
            optimizedFunctionResult.reenumerateNodes()
            context.log { "after removing redundant nodes:" }
            optimizedFunctionResult.logDigraph(context)

            val escapeAnalysisResult = EscapeAnalysisResult(
                    PointsToGraph(PointsToGraphForest(optimizedFunctionResult.graph.nodes.size), function)
                            .apply { copyFrom(optimizedFunctionResult.graph) },
                    optimizedFunctionResult.returnValue,
                    optimizedFunctionResult.objectsReferencedFromThrown
            )
            escapeAnalysisResult.simplifyFieldValues()
            context.log { "EA result for ${function.render()}" }
            escapeAnalysisResult.logDigraph(context)

            escapeAnalysisResults[function.symbol] = escapeAnalysisResult
            return true
        }

        private fun EscapeAnalysisResult.reenumerateNodes() {
            var id = 0
            val objectsReferencedFromThrown = mutableListOf<Node.Object>()
            for (index in graph.nodes.indices) {
                val node = graph.nodes[index]
                if (node != null) {
                    if (this.objectsReferencedFromThrown.get(node.id))
                        objectsReferencedFromThrown.add(node as Node.Object)
                    graph.nodes[id] = node
                    node.id = id++
                }
            }
            graph.nodes.trimSize(id)
            this.objectsReferencedFromThrown.clear()
            objectsReferencedFromThrown.forEach { this.objectsReferencedFromThrown.set(it.id) }
        }

        private fun EscapeAnalysisResult.removeUnreachable() {
            val visited = BitSet(graph.forest.totalNodes)
            visited.set(Node.NOTHING_ID)
            visited.set(Node.NULL_ID)
            visited.set(Node.UNIT_ID)

            fun findReachable(node: Node) {
                visited.set(node.id)
                node.forEachPointee { pointee ->
                    if (!visited.get(pointee.id)) findReachable(pointee)
                }
            }

            if (!visited.get(returnValue.id))
                findReachable(returnValue)
            findReachable(graph.globalNode)
            graph.parameterNodes.values.forEach {
                if (!visited.get(it.id)) findReachable(it)
            }
            for (id in 0 until graph.nodes.size) {
                val node = graph.nodes[id]
                if (!visited.get(id)) {
                    graph.nodes[id] = null
                } else {
                    (node as? Node.Reference)?.assignedTo?.removeAll { !visited.get(it.id) }
                }
            }
        }

        private fun EscapeAnalysisResult.bypassAndRemoveVariables() {
            val forbiddenToBypass = BitSet()
            forbiddenToBypass.set(returnValue.id)
            for (node in graph.nodes) {
                (node as? Node.Object)?.fields?.values?.forEach { forbiddenToBypass.set(it.id) }
            }
            for (id in 0 until graph.nodes.size) {
                val reference = graph.nodes[id] as? Node.Reference ?: continue
                if (forbiddenToBypass.get(reference.id)) continue
                require(reference.assignedTo.isNotEmpty())
                if (reference.assignedWith.isEmpty()
                        || (reference.assignedWith.size == 1 && reference.assignedWith[0] != reference /* Skip loops */)
                        || (reference.assignedTo.size == 1 && reference.assignedTo[0] != reference /* Skip loops */)
                ) {
                    graph.bypass(reference, NodeContext(Levels.FUNCTION, false, null, null))
                    graph.nodes[id] = null
//                    context.log { "after bypassing $reference:" }
//                    logDigraph(context)
                }
            }
        }

        // TODO: Rethink.
        private fun EscapeAnalysisResult.removeRedundantNodesPart1() {
            val visited = BitSet(graph.forest.totalNodes)

            fun findReachable(node: Node) {
                visited.set(node.id)
                node.forEachPointee { pointee ->
                    if (!visited.get(pointee.id)) findReachable(pointee)
                }
            }

            findReachable(graph.globalNode)
            val reachableFromGlobal = visited.copy()
            visited.clear()
            visited.set(Node.NOTHING_ID)
            visited.set(Node.NULL_ID)
            visited.set(Node.UNIT_ID)
            if (!visited.get(returnValue.id))
                findReachable(returnValue)
            graph.parameterNodes.values.forEach {
                if (!visited.get(it.id))
                    findReachable(it)
            }
            val reachableFromParameters = visited.copy()
            val reachableFromGlobalAndParameters = reachableFromParameters.copy().also { it.and(reachableFromGlobal) }

            reachableFromGlobal.forEachBit { id ->
                val node = graph.nodes[id]!!
                if (id == Node.GLOBAL_ID || reachableFromParameters.get(id) || node !is Node.Reference)
                    return@forEachBit
                val indices = IntArray(node.assignedWith.size)
                for (i in node.assignedWith.indices) {
                    val pointee = node.assignedWith[i]
                    if (!reachableFromParameters.get(pointee.id)) continue
                    var count = 0
                    for (j in i until node.assignedWith.size)
                        if (node.assignedWith[j] == pointee) {
                            indices[count++] = j
                            node.assignedWith[j] = graph.globalNode
                        }
                    visited.clear()
                    findReachable(graph.globalNode)
                    if (visited.get(pointee.id)) {
                        (pointee as? Node.Reference)?.assignedTo?.removeAll(node)
                    } else {
                        for (j in 0 until count)
                            node.assignedWith[indices[j]] = pointee
                    }
                }
                node.assignedWith.removeAll(graph.globalNode)
            }

            val removed = BitSet(graph.nodes.size)
            reachableFromGlobal.forEachBit { id ->
                if (id == Node.GLOBAL_ID || reachableFromParameters.get(id))
                    return@forEachBit
                visited.clear()
                visited.or(removed)
                visited.set(id)
                findReachable(graph.globalNode)
                visited.and(reachableFromGlobalAndParameters)
                if (visited.cardinality() == reachableFromGlobalAndParameters.cardinality()) {
                    removed.set(id)
                }
            }

            for (id in 0 until graph.nodes.size) {
                if (removed.get(id))
                    graph.nodes[id] = null
                else {
                    val node = graph.nodes[id] ?: continue
                    when (node) {
                        is Node.Object -> {
                            val removedFields = node.fields.filter { removed.get(it.value.id) }
                            removedFields.forEach { node.fields.remove(it.key) }
                        }
                        is Node.Reference -> {
                            node.assignedWith.removeAll { removed.get(it.id) }
                            node.assignedTo.removeAll { removed.get(it.id) }
                        }
                    }
                }
            }
        }

        // TODO: Remove incoming edges to Unit/Null/Nothing.
        private fun EscapeAnalysisResult.removeRedundantNodesPart2() {

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
                    is Node.Reference -> node.assignedWith.let { assignedWith ->
                        if (assignedWith.isEmpty())
                            leaves.add(node)
                        else
                            assignedWith.forEach { checkIncomingEdge(node, it.id) }
                    }
                }
            }

            val forbiddenToRemove = BitSet(graph.nodes.size)
            val removed = BitSet(graph.nodes.size)
            forbiddenToRemove.set(Node.NOTHING_ID)
            forbiddenToRemove.set(Node.NULL_ID)
            forbiddenToRemove.set(Node.UNIT_ID)
            forbiddenToRemove.set(Node.GLOBAL_ID)
            forbiddenToRemove.set(returnValue.id)
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
                    is Node.Reference -> singleNodePointingAtLeaf.assignedWith.let { assignedWith ->
                        assignedWith.remove(leafNode)
                        assignedWith.isEmpty()
                    }
                }
                if (hasBecomeALeaf)
                    leaves.push(singleNodePointingAtLeaf)
            }

            for (id in 0 until graph.nodes.size)
                if (removed.get(id))
                    graph.nodes[id] = null

            val referencesSets = arrayOfNulls<BitSet>(graph.nodes.size)
            for (node in graph.nodes) {
                val reference = node as? Node.Reference ?: continue
                for (pointee in reference.assignedWith) {
                    if (pointee is Node.Parameter || pointee !is Node.Object
                            || !pointee.isFictitious || pointee.fields.isNotEmpty()) continue
                    val set = (referencesSets[pointee.id] ?: BitSet().also { referencesSets[pointee.id] = it })
                    set.set(reference.id)
                }
            }
            removed.clear()
            val set = mutableSetOf<BitSet>()
            for (id in referencesSets.indices) {
                val referencesSet = referencesSets[id] ?: continue
                if (objectsReferencedFromThrown.get(id))
                    referencesSet.set(Node.GLOBAL_ID)
                if (!set.add(referencesSet)) {
                    removed.set(id)
                    graph.nodes[id] = null
                }
            }
            for (id in 0 until graph.nodes.size) {
                val reference = graph.nodes[id] as? Node.Reference ?: continue
                reference.assignedWith.removeAll { removed.get(it.id) }
                reference.assignedTo.removeAll { removed.get(it.id) }
            }
        }

        private fun EscapeAnalysisResult.simplifyFieldValues() {
            val fieldsWithMultipleValues = graph.nodes.filter {
                ((it as? Node.FieldValue)?.assignedWith?.size ?: 0) > 1
            }
            if (fieldsWithMultipleValues.isEmpty()) return

            fieldsWithMultipleValues.forEach { fieldValue ->
                val phiNode = graph.newPhiNode()
                (fieldValue as Node.FieldValue).assignedWith.forEach { fieldPointee ->
                    phiNode.assignedWith.add(fieldPointee)
                    if (fieldPointee is Node.Reference)
                        fieldPointee.assignedTo[fieldPointee.assignedTo.indexOf(fieldValue)] = phiNode
                }
                fieldValue.assignedWith.clear()
                fieldValue.assignedWith.add(phiNode)
            }
        }

        private val pointerSize = generationState.runtime.pointerSize
        private val symbols = context.ir.symbols

        private fun arrayItemSizeOf(irClass: IrClass): Int? = when (irClass.symbol) {
            symbols.array -> pointerSize
            symbols.booleanArray -> 1
            symbols.byteArray -> 1
            symbols.charArray -> 2
            symbols.shortArray -> 2
            symbols.intArray -> 4
            symbols.floatArray -> 4
            symbols.longArray -> 8
            symbols.doubleArray -> 8
            else -> null
        }

        private fun EscapeAnalysisResult.computeLifetimes(
                allocations: Map<IrFunctionAccessExpression, Int>,
                lifetimes: MutableMap<IrFunctionAccessExpression, Lifetime>,
        ) {
            propagateLevels()
            allocations.forEach { (ir, id) ->
                if (id >= graph.nodes.size || graph.nodes[id] == null) return@forEach // An allocation from unreachable code.
                val node = graph.nodes[id] as Node.Object
                val computedLifetime = lifetimeOf(node)
                var lifetime = computedLifetime

                if (lifetime != Lifetime.STACK) {
                    // TODO: Support Lifetime.LOCAL
                    lifetime = Lifetime.GLOBAL
                } else if (ir is IrConstructorCall) {
                    val constructedClass = ir.symbol.owner.constructedClass
                    val itemSize = arrayItemSizeOf(constructedClass)
                    if (itemSize != null) {
                        // TODO: Support arrays.
                        lifetime = Lifetime.GLOBAL
                    }
                }

                node.forcedLifetime = lifetime

                lifetimes[ir] = lifetime
            }
        }

        private fun EscapeAnalysisResult.propagateLevels() {
            val visited = BitSet()

            fun propagate(node: Node) {
                visited.set(node.id)
                val level = node.actualLevel
                node.forEachPointee {
                    if (!visited.get(it.id) && it.actualLevel >= level) {
                        it.actualLevel = level
                        propagate(it)
                    }
                }
            }

            returnValue.actualLevel = Levels.RETURN_VALUE
            visited.set(Node.NOTHING_ID)
            visited.set(Node.NULL_ID)
            visited.set(Node.UNIT_ID)

            val nodes = graph.nodes.filterNotNull().toTypedArray()
            nodes.sortBy { it.actualLevel }
            for (node in nodes) {
                if (!visited.get(node.id))
                    propagate(node)
            }
        }

    }

    fun computeLifetimes(
            context: Context,
            generationState: NativeGenerationState,
            callGraph: CallGraph,
            moduleDFG: ModuleDFG,
            lifetimes: MutableMap<IrElement, Lifetime>
    ) {
        try {
            InterproceduralAnalysis(context, generationState, callGraph, moduleDFG) {
                it.file.path.endsWith("z.kt")
            }.analyze(lifetimes)
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
