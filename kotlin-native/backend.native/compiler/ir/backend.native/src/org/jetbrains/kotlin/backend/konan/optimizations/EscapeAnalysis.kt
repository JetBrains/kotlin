/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DirectedGraphCondensationBuilder
import org.jetbrains.kotlin.backend.konan.DirectedGraphMultiNode
import org.jetbrains.kotlin.backend.konan.ir.annotations.Escapes
import org.jetbrains.kotlin.backend.konan.ir.annotations.PointsTo
import org.jetbrains.kotlin.backend.konan.ir.annotations.PointsToKind
import org.jetbrains.kotlin.backend.konan.ir.isBuiltInOperator
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.logMultiple
import org.jetbrains.kotlin.backend.konan.lower.originalConstructor
import org.jetbrains.kotlin.config.nativeBinaryOptions.GC
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.utils.atMostOne

private val DataFlowIR.Node.ir
    get() = when (this) {
        is DataFlowIR.Node.Call -> irCallSite
        is DataFlowIR.Node.Alloc -> irCallSite
        is DataFlowIR.Node.ArrayRead -> irCallSite
        is DataFlowIR.Node.FieldRead -> ir
        else -> null
    }

private val CallGraphNode.CallSite.arguments: List<DataFlowIR.Node>
    get() {
        return (0..call.arguments.size).map {
            if (it < call.arguments.size) call.arguments[it].node else node
        }
    }

internal object EscapeAnalysis {

    /*
     * The goal of escape analysis is to estimate lifetimes of all expressions in a program.
     * Possible lifetimes are:
     *   0. Stack        - an object is used only within its visibility scope within a function.
     *   1. Local        - an object is used only within a function.
     *   2. Return value - an object is either returned or set to a field of an object being returned.
     *   3. Parameter    - an object is set to a field of some parameters of a function.
     *   4. Global       - otherwise.
     * For now only Stack and Global lifetimes are supported by the codegen, so others will be pulled up to Global.
     *
     * The analysis is performed in two main stages - intraprocedural and interprocedural.
     * During intraprocedural analysis we remove all control flow related expressions and compute all possible
     * values of all variables within a function.
     * The goal of interprocedural analysis is to build points-to graph (object A references object B if and only if
     * there is a path from the node A to the node B). This is done by building call graph (using devirtualization
     * for more precise result). But in practice holding this condition both ways can be difficult and bad in terms of
     * performance, so the algorithm tries to ensure only one part: if object A references object B then there must be
     * a path from the node A to the node B, with that none of the constraints from the original program will be lost.
     * It is ok to add some additional constraints, as long as there are not too many of those.
     *
     * How do we exactly build the points-to graph out of the call graph?
     * 1. Build condensation of the call graph.
     * 2. Handle vertices of the resulting DAG in the reversed topological order (ensuring that all functions being called
     *    are already handled).
     * 3. For a strongly connected component build the points-to graph iteratively starting with empty graph
     *    (if the process is seemed to not be converging for some function, assume the pessimistic result).
     *
     * Escape analysis result of a function is not only lifetimes for all allocations of that function
     * but also a snippet of its points-to graph (it's a reduced version, basically, subgraph reachable from
     * the function's parameters).
     * Assuming the function has parameters P0, P1, .., Pn, where the last parameter is the return parameter,
     * it turns out that the snippet can be described as an array of relations of form
     *   v.f0.f1...fk -> w.g0.g1...gl where v, w - either one of the function's parameters or special
     * additional nodes called drains which will be introduced later; and f0, f1, .., fk, g0, g1, .., gl - fields.
     *
     * Building points-to graph:
     * 1. Seed it from the function's DataFlowIR.
     *     There are two kinds of edges:
     *         1) field. The subgraph for [a.f]:
     *               [a]
     *                | f
     *                V
     *              [a.f]
     *            Notice the label [f] on the edge.
     *         2) assignment. The subgraph for [a = b]:
     *               [a]
     *                |
     *                V
     *               [b]
     *            No labels on the edge.
     *     When calling a function, take its points-to graph snippet and embed it at the call site,
     *     replacing parameters with actual node arguments.
     * 2. Build the closure.
     *     Consider an assignment [a = b], and a usage of [a.f] somewhere. Since there is no order on nodes
     *     of DataFlowIR (sea of nodes), the conservative assumption has to be made - [b.f] is also being used
     *     at the same place as [a.f] is. Same applies for usages of [b.f].
     *     This reasoning leads to the following algorithm:
     *         Consider for the time being all assignment edges undirected and build connected components.
     *         Now, every field usage of any node within a component implies the same usage of any other node
     *         from that component, so the following transformation will be performed:
     *             1) Consider components one by one. Select a node which has no outgoing assignment edges,
     *                if there is no such a node, create additional node and add assignment edges from every node
     *                to it. Call this node a drain. Then move all beginnings of field edges from all nodes to
     *                the drain leaving the ends as is (this reflects the above consideration - any field usage
     *                can be applied to any node within a component).
     *             2) After drains creation and field edges moving there might emerge multi-edges (more than one
     *                field edge with the same label going to different components). The components these
     *                multi-edges are pointing at must be coalesced together (this is done either by creating
     *                a new drain or connecting one component's drain to the other). This operation must be
     *                performed until there are no more multi-edges.
     *     After the above transformation has been made, finally, simple lifetime propagation can be performed,
     *     seeing all edges directed.
     */

    // A special marker field for external types implemented in the runtime (mainly, arrays).
    // The types being passed to the constructor are not used in the analysis - just put there anything.
    private val intestinesField = DataFlowIR.Field(DataFlowIR.Type.Virtual, -1, "inte\$tines")

    // A special marker field for return values.
    // Basically we substitute [return x] with [ret.v@lue = x].
    // This is done in order to not handle return parameter somewhat specially.
    private val returnsValueField = DataFlowIR.Field(DataFlowIR.Type.Virtual, -2, "v@lue")

    // The less the higher an object escapes.
    object Depths {
        val INFINITY = 1_000_000
        val ROOT_SCOPE = 0
        val RETURN_VALUE = -1
        val PARAMETER = -2
        val GLOBAL = -3
    }

    object DivergenceResolutionParams {
        const val MaxAttempts = 3
        const val NegligibleSize = 100
        const val SwellingFactor = 25
    }

    private inline fun <reified T: Comparable<T>> Array<T>.sortedAndDistinct(): Array<T> {
        this.sort()
        if (this.isEmpty()) return this
        val unique = mutableListOf(this[0])
        for (i in 1 until this.size)
            if (this[i] != this[i - 1])
                unique.add(this[i])
        return unique.toTypedArray()
    }

    private class CompressedPointsToGraph(edges: Array<Edge>) {
        val edges = edges.sortedAndDistinct()

        sealed class NodeKind {
            abstract val absoluteIndex: Int

            object Return : NodeKind() {
                override val absoluteIndex = 0

                override fun equals(other: Any?) = other === this

                override fun toString() = "RET"
            }

            class Param(val index: Int) : NodeKind() {
                override val absoluteIndex: Int
                    get() = -1_000_000 + index

                override fun equals(other: Any?) = index == (other as? Param)?.index

                override fun toString() = "P$index"
            }

            class Drain(val index: Int) : NodeKind() {
                override val absoluteIndex: Int
                    get() = index + 1

                override fun equals(other: Any?) = index == (other as? Drain)?.index

                override fun toString() = "D$index"
            }

            companion object {
                fun parameter(index: Int, total: Int) =
                        if (index == total - 1)
                            Return
                        else
                            Param(index)
            }
        }

        class Node(val kind: NodeKind, val path: Array<DataFlowIR.Field>) : Comparable<Node> {
            override fun compareTo(other: Node): Int {
                if (kind.absoluteIndex != other.kind.absoluteIndex)
                    return kind.absoluteIndex.compareTo(other.kind.absoluteIndex)
                for (i in path.indices) {
                    if (i >= other.path.size)
                        return 1
                    if (path[i].index != other.path[i].index)
                        return path[i].index.compareTo(other.path[i].index)
                }
                if (path.size < other.path.size) return -1
                return 0
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Node) return false
                if (kind != other.kind || path.size != other.path.size)
                    return false
                for (i in path.indices)
                    if (path[i] != other.path[i])
                        return false
                return true
            }

            override fun toString() = debugString(null)

            fun debugString(root: String?) = buildString {
                append(root ?: kind.toString())
                path.forEach {
                    append('.')
                    append(it.name ?: "<no_name@${it.index}>")
                }
            }

            fun goto(field: DataFlowIR.Field?) = when (field) {
                null -> this
                else -> Node(kind, Array(path.size + 1) { if (it < path.size) path[it] else field })
            }

            companion object {
                fun parameter(index: Int, total: Int) = Node(NodeKind.parameter(index, total), path = emptyArray())
                fun drain(index: Int) = Node(NodeKind.Drain(index), path = emptyArray())
            }
        }

        class Edge(val from: Node, val to: Node) : Comparable<Edge> {
            override fun compareTo(other: Edge): Int {
                val fromCompareResult = from.compareTo(other.from)
                if (fromCompareResult != 0)
                    return fromCompareResult
                return to.compareTo(other.to)
            }

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is Edge) return false
                return from == other.from && to == other.to
            }

            override fun toString() = "$from -> $to"

            companion object {
                fun pointsTo(param1: Int, param2: Int, totalParams: Int, kind: PointsToKind): Edge {
                    val from = if (kind.sourceIsDirect)
                        Node.parameter(param1, totalParams)
                    else
                        Node(NodeKind.parameter(param1, totalParams), Array(1) { intestinesField })
                    val to = if (kind.destinationIsDirect)
                        Node.parameter(param2, totalParams)
                    else
                        Node(NodeKind.parameter(param2, totalParams), Array(1) { intestinesField })
                    return Edge(from, to)
                }
            }
        }
    }

    private class FunctionEscapeAnalysisResult(
            val numberOfDrains: Int,
            val pointsTo: CompressedPointsToGraph,
            escapes: Array<CompressedPointsToGraph.Node>
    ) {
        val escapes = escapes.sortedAndDistinct()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FunctionEscapeAnalysisResult) return false

            if (escapes.size != other.escapes.size) return false
            for (i in escapes.indices)
                if (escapes[i] != other.escapes[i]) return false

            if (pointsTo.edges.size != other.pointsTo.edges.size)
                return false
            for (i in pointsTo.edges.indices)
                if (pointsTo.edges[i] != other.pointsTo.edges[i])
                    return false
            return true
        }

        override fun toString(): String {
            val result = StringBuilder()
            result.appendLine("PointsTo:")
            pointsTo.edges.forEach { result.appendLine("    $it") }
            result.append("Escapes:")
            escapes.forEach {
                result.append(' ')
                result.append(it)
            }
            return result.toString()
        }

        companion object {
            fun fromAnnotations(escapesAnnotation: Escapes?, pointsToAnnotation: PointsTo?, numberOfParameters: Int): FunctionEscapeAnalysisResult {
                escapesAnnotation?.run {
                    assertIsValidFor(numberOfParameters + 1)
                }
                pointsToAnnotation?.run {
                    assertIsValidFor(numberOfParameters + 1)
                }
                val edges = mutableListOf<CompressedPointsToGraph.Edge>()
                val escapes = mutableListOf<CompressedPointsToGraph.Node>()
                for (paramFrom in 0..numberOfParameters) {
                    if (escapesAnnotation?.escapesAt(paramFrom) == true)
                        escapes.add(CompressedPointsToGraph.Node.parameter(paramFrom, numberOfParameters + 1))
                    pointsToAnnotation?.run {
                        for (paramTo in 0..numberOfParameters) {
                            kind(paramFrom, paramTo)?.let {
                                edges.add(CompressedPointsToGraph.Edge.pointsTo(paramFrom, paramTo, numberOfParameters + 1, it))
                            }
                        }
                    }
                }
                return FunctionEscapeAnalysisResult(
                        0, CompressedPointsToGraph(edges.toTypedArray()), escapes.toTypedArray())
            }

            fun optimistic() =
                    FunctionEscapeAnalysisResult(0, CompressedPointsToGraph(emptyArray()), emptyArray())

            fun pessimistic(numberOfParameters: Int) =
                    FunctionEscapeAnalysisResult(0, CompressedPointsToGraph(emptyArray()),
                            Array(numberOfParameters + 1) { CompressedPointsToGraph.Node.parameter(it, numberOfParameters + 1) })
        }
    }

    private class InterproceduralAnalysis(
            val context: Context,
            val generationState: NativeGenerationState,
            val callGraph: CallGraph,
            val moduleDFG: ModuleDFG,
            val lifetimes: MutableMap<IrElement, Lifetime>,
            val propagateExiledToHeapObjects: Boolean
    ) {

        private val symbols = context.symbols
        private val throwable = symbols.throwable.owner

        val escapeAnalysisResults = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, FunctionEscapeAnalysisResult>()

        fun analyze() {
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

            for (functionSymbol in callGraph.directEdges.keys) {
                if (!moduleDFG.functions.containsKey(functionSymbol)) continue
                // Assume trivial result at the beginning - then iteratively specify it.
                escapeAnalysisResults[functionSymbol] = FunctionEscapeAnalysisResult.optimistic()
            }

            for (multiNode in condensation.topologicalOrder.reversed()) {
                analyze(callGraph, multiNode)
            }

            context.logMultiple {
                with(stats) {
                    +"Managed to alloc on stack"
                    +("    everything (including globals): $totalStackAllocsCount out of ${totalGlobalAllocsCount + totalStackAllocsCount}" +
                            " (${totalStackAllocsCount * 100.0 / (totalGlobalAllocsCount + totalStackAllocsCount)}%)")
                    +("    interesting (without globals) : $filteredStackAllocsCount out of ${filteredGlobalAllocsCount + filteredStackAllocsCount}" +
                            " (${filteredStackAllocsCount * 100.0 / (filteredGlobalAllocsCount + filteredStackAllocsCount)}%)")
                    +"Total ea result size: $totalEAResultSize"
                    +"Total points-to graph size: $totalPTGSize"
                    +"Total data flow graph size: $totalDFGSize"
                }
            }
        }

        private enum class ComputationState {
            NEW,
            PENDING,
            DONE
        }

        private class Stats {
            var totalGlobalAllocsCount = 0
            var totalStackAllocsCount = 0
            var filteredGlobalAllocsCount = 0
            var filteredStackAllocsCount = 0
            var totalEAResultSize = 0
            var totalPTGSize = 0
            var totalDFGSize = 0
        }

        private val stats = Stats()

        private fun maxPointsToGraphSizeOf(function: DataFlowIR.FunctionSymbol) = with(DivergenceResolutionParams) {
            // A heuristic: the majority of functions have their points-to graph size linear in number of IR (or DFG) nodes,
            // there are exceptions but it's a trade-off we have to make.
            // The trick with [NegligibleSize] handles functions that basically delegate their work to other functions.
            val numberOfNodes = moduleDFG.functions[function]!!.body.allScopes.sumOf { it.nodes.size }
            NegligibleSize + numberOfNodes * SwellingFactor
        }

        private fun analyze(callGraph: CallGraph, multiNode: DirectedGraphMultiNode<DataFlowIR.FunctionSymbol.Declared>) {
            val nodes = multiNode.nodes.filter { moduleDFG.functions.containsKey(it) }.toMutableSet()

            context.logMultiple {
                +"Analyzing multiNode:\n    ${nodes.joinToString("\n   ") { it.toString() }}"
                nodes.forEach { from ->
                    +"DataFlowIR"
                    moduleDFG.functions[from]!!.debugOutput()
                    callGraph.directEdges[from]!!.callSites.forEach { to ->
                        +"CALL"
                        +"   from $from"
                        +"   to ${to.actualCallee}"
                    }
                }
            }

            var pointsToGraphs = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, PointsToGraph>()
            var failedToConverge = false
            val toAnalyze = mutableSetOf<DataFlowIR.FunctionSymbol.Declared>()
            toAnalyze.addAll(nodes)
            val numberOfRuns = nodes.associateWith { 0 }.toMutableMap()
            while (!failedToConverge && toAnalyze.isNotEmpty()) {
                val function = toAnalyze.first()
                toAnalyze.remove(function)
                numberOfRuns[function] = numberOfRuns[function]!! + 1
                context.log { "Processing function $function" }

                val startResult = escapeAnalysisResults[function]!!
                context.log { "Start escape analysis result:\n$startResult" }

                val pointsToGraph = PointsToGraph(function)
                pointsToGraphs[function] = pointsToGraph

                if (!analyze(callGraph.directEdges[function]!!.callSites, pointsToGraph, function, maxPointsToGraphSizeOf(function))) {
                    failedToConverge = true
                } else {
                    val endResult = escapeAnalysisResults[function]!!
                    if (startResult == endResult) {
                        context.log { "Escape analysis is not changed" }
                    } else {
                        context.log { "Escape analysis was refined:\n$endResult" }
                        if (numberOfRuns[function]!! > DivergenceResolutionParams.MaxAttempts)
                            failedToConverge = true
                        else {
                            callGraph.reversedEdges[function]?.forEach {
                                if (nodes.contains(it))
                                    toAnalyze.add(it)
                            }
                        }
                    }
                }

                if (failedToConverge)
                    context.log { "WARNING: Escape analysis for $function seems not to be converging. Falling back to conservative strategy." }
            }

            if (failedToConverge) {
                pointsToGraphs = analyzeComponentPessimistically(callGraph, multiNode)
            }

            pointsToGraphs.forEach { (function, graph) ->
                val eaResult = escapeAnalysisResults[function]!!
                stats.totalEAResultSize += eaResult.numberOfDrains + eaResult.escapes.size + eaResult.pointsTo.edges.size

                stats.totalPTGSize += graph.allNodes.size
                stats.totalDFGSize += moduleDFG.functions[function]!!.body.allScopes.sumOf { it.nodes.size }

                val functionSymbol = graph.functionSymbol
                for (node in graph.nodes.keys) {
                    node.ir?.let {
                        val lifetime = graph.lifetimeOf(node)

                        // Filter out some irrelevant cases (like globals and exceptions).
                        val isFilteredOut = functionSymbol.isStaticFieldInitializer ||
                                functionSymbol.irFunction?.originalConstructor?.constructedClass?.kind?.let { kind ->
                                    kind == ClassKind.ENUM_CLASS || kind == ClassKind.OBJECT
                                } == true ||
                                (node as? DataFlowIR.Node.Alloc)?.type?.irClass?.getAllSuperclasses()?.contains(throwable) == true

                        if (node is DataFlowIR.Node.Alloc) {
                            if (lifetime == Lifetime.GLOBAL) {
                                ++stats.totalGlobalAllocsCount
                                if (!isFilteredOut)
                                    ++stats.filteredGlobalAllocsCount
                            }
                            if (lifetime == Lifetime.STACK) {
                                ++stats.totalStackAllocsCount
                                if (!isFilteredOut)
                                    ++stats.filteredStackAllocsCount
                            }

                            lifetimes[it] = lifetime
                        }
                    }
                }
            }
        }

        private fun analyzeComponentPessimistically(
                callGraph: CallGraph,
                multiNode: DirectedGraphMultiNode<DataFlowIR.FunctionSymbol.Declared>
        ): MutableMap<DataFlowIR.FunctionSymbol.Declared, PointsToGraph> {
            val nodes = multiNode.nodes.filter { moduleDFG.functions.containsKey(it) }
            val pointsToGraphs = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, PointsToGraph>()
            val computationStates = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, ComputationState>()
            nodes.forEach { computationStates[it] = ComputationState.NEW }
            val toAnalyze = nodes.toMutableList()
            while (toAnalyze.isNotEmpty()) {
                val function = toAnalyze.peek()!!
                val state = computationStates[function]!!
                val callSites = callGraph.directEdges[function]!!.callSites
                when (state) {
                    ComputationState.NEW -> {
                        computationStates[function] = ComputationState.PENDING
                        for (callSite in callSites) {
                            val callee = callSite.actualCallee
                            val calleeComputationState = computationStates[callee]
                            if (callSite.isVirtual
                                    || callee !is DataFlowIR.FunctionSymbol.Declared // An external call.
                                    || calleeComputationState == null // A call to a function from other component.
                                    || calleeComputationState == ComputationState.DONE // Already analyzed.
                            ) {
                                continue
                            }

                            if (calleeComputationState == ComputationState.PENDING) {
                                // A cycle - break it by assuming nothing about the callee.
                                // This is not the callee's final result - it will be recomputed later in the loop.
                                escapeAnalysisResults[callee] = FunctionEscapeAnalysisResult.pessimistic(callee.parameters.size)
                            } else {
                                computationStates[callee] = ComputationState.NEW
                                toAnalyze.push(callee)
                            }
                        }
                    }

                    ComputationState.PENDING -> {
                        toAnalyze.pop()
                        computationStates[function] = ComputationState.DONE
                        val pointsToGraph = PointsToGraph(function)
                        if (analyze(callSites, pointsToGraph, function, maxPointsToGraphSizeOf(function)))
                            pointsToGraphs[function] = pointsToGraph
                        else {
                            // TODO: suboptimal. May be it is possible somehow handle the entire component at once?
                            context.log {
                                "WARNING: Escape analysis for $function seems not to be converging." +
                                        " Assuming conservative results."
                            }
                            escapeAnalysisResults[function] = FunctionEscapeAnalysisResult.pessimistic(function.parameters.size)
                            // Invalidate the points-to graph.
                            pointsToGraphs[function] = PointsToGraph(function).apply {
                                allNodes.forEach { it.depth = Depths.GLOBAL }
                            }
                        }
                    }

                    ComputationState.DONE -> {
                        toAnalyze.pop()
                    }
                }
            }

            return pointsToGraphs
        }

        private fun arrayLengthOf(node: DataFlowIR.Node): Int? {
            var lengthNode: DataFlowIR.Node.SimpleConst<*>? = null
            val nodes = mutableListOf(node)
            val visited = mutableSetOf<DataFlowIR.Node>()
            while (true) {
                val currentNode = nodes.peek() ?: break
                nodes.pop()
                visited.add(currentNode)
                when (currentNode) {
                    is DataFlowIR.Node.SimpleConst<*> -> {
                        if (lengthNode != null && currentNode != lengthNode)
                            return null
                        lengthNode = currentNode
                    }
                    is DataFlowIR.Node.Variable -> {
                        currentNode.values.forEach {
                            val nextNode = it.node
                            if (nextNode !in visited)
                                nodes.push(nextNode)
                        }
                    }
                    else -> return null
                }
            }

            return lengthNode?.value as? Int
        }

        private val pointerSize = generationState.runtime.pointerSize

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

        private fun arraySizeInBytes(itemSize: Int, length: Int): Long =
                pointerSize /* typeinfo */ + 4 /* size */ + itemSize * length.toLong()

        private fun analyze(
                callSites: List<CallGraphNode.CallSite>,
                pointsToGraph: PointsToGraph,
                function: DataFlowIR.FunctionSymbol.Declared,
                maxAllowedGraphSize: Int
        ): Boolean {
            context.log { "Before calls analysis" }
            pointsToGraph.log()
            pointsToGraph.logDigraph(false)

            callSites.forEach {
                val callee = it.actualCallee
                val calleeEAResult = if (it.isVirtual)
                    getExternalFunctionEAResult(it)
                else
                    callGraph.directEdges[callee]?.let { escapeAnalysisResults[it.symbol]!! }
                            ?: getExternalFunctionEAResult(it)
                pointsToGraph.processCall(it, calleeEAResult)

                if (pointsToGraph.allNodes.size > maxAllowedGraphSize)
                    return false
            }

            context.log { "After calls analysis" }
            pointsToGraph.log()
            pointsToGraph.logDigraph(false)

            // Build transitive closure.
            val eaResult = pointsToGraph.buildClosure()

            context.log { "After closure building" }
            pointsToGraph.log()
            pointsToGraph.logDigraph(true)

            escapeAnalysisResults[function] = eaResult

            return true
        }

        private fun getExternalFunctionEAResult(callSite: CallGraphNode.CallSite): FunctionEscapeAnalysisResult {
            val callee = callSite.actualCallee

            val calleeEAResult = if (callSite.isVirtual) {
                context.log { "A virtual call: $callee" }
                FunctionEscapeAnalysisResult.pessimistic(callee.parameters.size)
            } else {
                context.log { "An external call: $callee" }
                if (callee.name?.startsWith("kfun:kotlin.") == true
                        // TODO: Is it possible to do it in a more fine-grained fashion?
                        && !callee.name.startsWith("kfun:kotlin.native.concurrent")
                        && !callee.name.startsWith("kfun:kotlin.concurrent")) {
                    context.log { "A function from K/N runtime - can use annotations" }
                    if (callee.irFunction?.isBuiltInOperator == false) { // If callee is a function, but not a builtin operator
                        if (callee.escapes == null && callee.pointsTo == null) { // and it had no EA annotations
                            require(callee.parameters.all { it.type.irClass == null }) { // all of its parameters must be primitive types
                                "Function ${callee.name} has reference parameters and is missing EA annotations. Must have been handled by SpecialBackendChecksTraversal"
                            }
                            require(callee.returnsUnit || callee.returnsNothing || callee.returnParameter.type.irClass == null) { // as well as the return value
                                "Function ${callee.name} has reference return value and is missing EA annotations. Must have been handled by SpecialBackendChecksTraversal"
                            }
                        }
                    }
                    FunctionEscapeAnalysisResult.fromAnnotations(callee.escapes, callee.pointsTo, callee.parameters.size)
                } else {
                    context.log { "An unknown function - assume pessimistic result" }
                    FunctionEscapeAnalysisResult.pessimistic(callee.parameters.size)
                }
            }

            context.logMultiple {
                +"Escape analysis result"
                +calleeEAResult.toString()
                +""
            }
            return calleeEAResult
        }

        private enum class PointsToGraphNodeKind {
            STACK,
            LOCAL,
            PARAMETER,
            RETURN_VALUE,
            GLOBAL
        }

        private sealed class PointsToGraphEdge(val node: PointsToGraphNode) {
            class Assignment(node: PointsToGraphNode) : PointsToGraphEdge(node)

            class Field(node: PointsToGraphNode, val field: DataFlowIR.Field) : PointsToGraphEdge(node)
        }

        private class PointsToGraphNode(val startDepth: Int, val node: DataFlowIR.Node?) {
            val edges = mutableListOf<PointsToGraphEdge>()
            val reversedEdges = mutableListOf<PointsToGraphEdge.Assignment>()

            fun addAssignmentEdge(to: PointsToGraphNode) {
                edges += PointsToGraphEdge.Assignment(to)
                to.reversedEdges += PointsToGraphEdge.Assignment(this)
            }

            private val fields = mutableMapOf<DataFlowIR.Field, PointsToGraphNode>()

            fun getFieldNode(field: DataFlowIR.Field, graph: PointsToGraph) =
                    fields.getOrPut(field) { graph.newNode().also { edges += PointsToGraphEdge.Field(it, field) } }

            var depth = startDepth

            val kind
                get() = when {
                    depth == Depths.GLOBAL -> PointsToGraphNodeKind.GLOBAL
                    depth == Depths.PARAMETER -> PointsToGraphNodeKind.PARAMETER
                    depth == Depths.RETURN_VALUE -> PointsToGraphNodeKind.RETURN_VALUE
                    depth != startDepth -> PointsToGraphNodeKind.LOCAL
                    else -> PointsToGraphNodeKind.STACK
                }

            var forcedLifetime: Lifetime? = null

            lateinit var drain: PointsToGraphNode
            val isDrain get() = this == drain

            val actualDrain: PointsToGraphNode
                get() = drain.let {
                    if (it.isDrain) it
                    // Flip to the real drain as it is done in the disjoint sets algorithm,
                    // to reduce the time spent in this function.
                    else it.actualDrain.also { drain = it }
                }
            val isActualDrain get() = this == actualDrain
        }

        private data class ArrayStaticAllocation(val node: PointsToGraphNode, val irClass: IrClass, val length: Int, val sizeInBytes: Int)

        private enum class EdgeDirection {
            FORWARD,
            BACKWARD
        }

        private inner class PointsToGraph(val functionSymbol: DataFlowIR.FunctionSymbol) {
            val function = moduleDFG.functions[functionSymbol]!!
            val nodes = mutableMapOf<DataFlowIR.Node, PointsToGraphNode>()

            val allNodes = mutableListOf<PointsToGraphNode>()

            fun newNode(depth: Int, node: DataFlowIR.Node?) =
                    PointsToGraphNode(depth, node).also { allNodes.add(it) }
            fun newNode() = newNode(Depths.INFINITY, null)
            fun newDrain() = newNode().also { it.drain = it }

            val returnsNode = newNode(Depths.RETURN_VALUE, null)

            /*
             * Of all escaping nodes there are some "starting" - call them origins.
             * Consider a variable [v], which is assigned with two values - [a] and [b].
             * Now assume [a] escapes (written to a global, for instance). This implies that [v] also escapes,
             * but [b] doesn't, albeit [v] (an escaping node) references it. It's because [v] is not an escape origin.
             */
            // The origins of escaping.
            val escapeOrigins = mutableSetOf<PointsToGraphNode>()
            // Nodes reachable from either of escape origins going along all edges (assignment and/or field).
            val reachableFromEscapeOrigins = mutableSetOf<PointsToGraphNode>()
            // Nodes referencing any escape origin only by assignment edges.
            val referencingEscapeOrigins = mutableSetOf<PointsToGraphNode>()

            fun escapes(node: PointsToGraphNode) = node in reachableFromEscapeOrigins || node in referencingEscapeOrigins

            val ids = (listOf(function.body.rootScope) + function.body.allScopes.flatMap { it.nodes })
                    .withIndex().associateBy({ it.value }, { it.index })

            fun lifetimeOf(node: DataFlowIR.Node) = nodes[node]!!.let { it.forcedLifetime ?: lifetimeOf(it) }

            fun lifetimeOf(node: PointsToGraphNode) =
                    if (escapes(node))
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
                            when {
                                // If a value is explicitly returned.
                                node.node?.let { it in returnValues } == true -> Lifetime.RETURN_VALUE

                                // A value is stored into a field of the return value.
                                else -> Lifetime.INDIRECT_RETURN_VALUE
                            }
                        }
                    }

            private val returnValues: Set<DataFlowIR.Node> = function.body.returns.values.map { it.node }.toSet()

            init {
                fun traverseAndConvert(node: DataFlowIR.Node, depth: Int) {
                    when (node) {
                        DataFlowIR.Node.Null -> return
                        is DataFlowIR.Node.Scope -> node.nodes.forEach { traverseAndConvert(it, depth + 1) }
                        else -> {
                            val actualDepth = when (node) {
                                is DataFlowIR.Node.Parameter -> Depths.PARAMETER
                                in returnValues -> Depths.RETURN_VALUE
                                else -> depth
                            }
                            nodes[node] = newNode(actualDepth, node)
                        }
                    }
                }

                val body = function.body

                traverseAndConvert(body.rootScope, Depths.ROOT_SCOPE - 1)

                val dummyParameter = DataFlowIR.Node.Parameter(-1)
                val parameters = Array(function.symbol.parameters.size) { dummyParameter } // Put dummy in order to not bother with nullability.
                // Parameters are declared in the root scope
                for (node in body.rootScope.nodes) {
                    (node as? DataFlowIR.Node.Parameter)?.let {
                        require(parameters[it.index] == dummyParameter) {
                            "Two different parameters with the same index ${it.index} for $functionSymbol"
                        }
                        parameters[it.index] = it
                    }
                }
                parameters.forEachIndexed { index, parameter ->
                    require(parameter != dummyParameter) {
                        "No parameter with index $index for $functionSymbol"
                    }
                }

                val nothing = moduleDFG.symbolTable.mapClassReferenceType(context.symbols.nothing.owner)
                body.forEachNonScopeNode { node ->
                    when (node) {
                        is DataFlowIR.Node.FieldWrite -> {
                            val receiverNode = node.receiver?.node
                            // Here and below the receiver node might be null due to unreachable code
                            // (including the one having appeared after the inlining phase).
                            if (node.value.node != DataFlowIR.Node.Null && receiverNode != DataFlowIR.Node.Null) {
                                val receiver = receiverNode?.let { nodes[it]!! }
                                val value = nodes[node.value.node]!!
                                if (receiver == null)
                                    escapeOrigins.add(value)
                                else
                                    receiver.getFieldNode(node.field, this).addAssignmentEdge(value)
                            }
                        }

                        is DataFlowIR.Node.Singleton -> {
                            val type = node.type
                            if (type != nothing)
                                escapeOrigins.add(nodes[node]!!)
                        }

                        is DataFlowIR.Node.FieldRead -> {
                            val receiverNode = node.receiver?.node
                            if (receiverNode != DataFlowIR.Node.Null) {
                                val readResult = nodes[node]!!
                                val receiver = receiverNode?.let { nodes[it]!! }
                                if (receiver == null)
                                    escapeOrigins.add(readResult)
                                else
                                    readResult.addAssignmentEdge(receiver.getFieldNode(node.field, this))
                            }
                        }

                        is DataFlowIR.Node.ArrayWrite -> {
                            val arrayNode = node.array.node
                            val valueNode = node.value.node
                            if (valueNode != DataFlowIR.Node.Null && arrayNode != DataFlowIR.Node.Null) {
                                val array = nodes[arrayNode]!!
                                val value = nodes[valueNode]!!
                                array.getFieldNode(intestinesField, this).addAssignmentEdge(value)
                            }
                        }

                        is DataFlowIR.Node.ArrayRead -> {
                            val arrayNode = node.array.node
                            if (arrayNode != DataFlowIR.Node.Null) {
                                val array = nodes[arrayNode]!!
                                val readResult = nodes[node]!!
                                readResult.addAssignmentEdge(array.getFieldNode(intestinesField, this))
                            }
                        }

                        is DataFlowIR.Node.SaveCoroutineState -> {
                            val thisIntestines = nodes[parameters[0]]!!.getFieldNode(intestinesField, this)
                            for (variable in node.liveVariables)
                                thisIntestines.addAssignmentEdge(nodes[variable]!!)
                        }

                        is DataFlowIR.Node.Variable -> {
                            val variable = nodes[node]!!
                            for (value in node.values) {
                                if (value.node != DataFlowIR.Node.Null)
                                    variable.addAssignmentEdge(nodes[value.node]!!)
                            }
                        }
                        is DataFlowIR.Node.Alloc,
                        is DataFlowIR.Node.Call,
                        is DataFlowIR.Node.Const,
                        is DataFlowIR.Node.FunctionReference,
                        is DataFlowIR.Node.Null,
                        is DataFlowIR.Node.Parameter,
                        is DataFlowIR.Node.Scope -> Unit
                    }
                }

                body.throws.values.forEach { escapeOrigins.add(nodes[it.node]!!) }
                body.returns.values.forEach {
                    if (it.node != DataFlowIR.Node.Null)
                        returnsNode.getFieldNode(returnsValueField, this).addAssignmentEdge(nodes[it.node]!!)
                }

                val escapes = functionSymbol.escapes
                if (escapes != null) {
                    for (parameter in parameters)
                        if (escapes.escapesAt(parameter.index))
                            escapeOrigins += nodes[parameter]!!
                    if (escapes.escapesAt(parameters.size))
                        escapeOrigins += returnsNode
                }
            }

            private fun nodeToStringWhole(node: DataFlowIR.Node) = DataFlowIR.Function.nodeToString(node, ids)

            private fun nodeToString(node: DataFlowIR.Node) = ids[node].toString()

            fun log() = context.logMultiple {
                +"POINTS-TO GRAPH"
                +"NODES"
                val tempIds = mutableMapOf<PointsToGraphNode, Int>()
                var tempIndex = 0
                allNodes.forEach {
                    if (it.node == null)
                        tempIds[it] = tempIndex++
                }
                allNodes.forEach {
                    val tempId = tempIds[it]
                    +"    ${lifetimeOf(it)} ${it.depth} ${if (it in escapeOrigins) "ESCAPES" else ""} ${it.node?.let { nodeToString(it) } ?: "t$tempId"}"
                    +(it.node?.let { nodeToStringWhole(it) } ?: "        t$tempId")
                }
            }

            fun logDigraph(
                    markDrains: Boolean,
                    nodeFilter: (PointsToGraphNode) -> Boolean = { true },
                    nodeLabel: ((PointsToGraphNode) -> String)? = null
            ) = context.logMultiple {
                +"digraph {"
                val tempIds = mutableMapOf<PointsToGraphNode, Int>()
                var tempIndex = 0
                allNodes.forEach {
                    if (it.node == null)
                        tempIds[it] = tempIndex++
                }

                fun PointsToGraphNode.format() =
                        (nodeLabel?.invoke(this) ?:
                        (if (markDrains && isDrain) "d" else "")
                        + (node?.let { "n${ids[it]!!}" } ?: "t${tempIds[this]}")) +
                                "[d=$depth,${if (this in escapeOrigins) "eo" else if (escapes(this)) "e" else ""}]"

                for (from in allNodes) {
                    if (!nodeFilter(from)) continue
                    for (it in from.edges) {
                        val to = it.node
                        if (!nodeFilter(to)) continue
                        when (it) {
                            is PointsToGraphEdge.Assignment ->
                                +"    \"${from.format()}\" -> \"${to.format()}\";"
                            is PointsToGraphEdge.Field ->
                                +"    \"${from.format()}\" -> \"${to.format()}\" [ label=\"${it.field.name}\"];"
                        }
                    }
                }
                +"}"
            }

            fun processCall(callSite: CallGraphNode.CallSite, calleeEscapeAnalysisResult: FunctionEscapeAnalysisResult) {
                val call = callSite.call
                context.logMultiple {
                    +"Processing callSite"
                    +nodeToStringWhole(call)
                    +"Actual callee: ${callSite.actualCallee}"
                    +"Callee escape analysis result:"
                    +calleeEscapeAnalysisResult.toString()
                }

                val arguments = callSite.arguments

                val calleeDrains = Array(calleeEscapeAnalysisResult.numberOfDrains) { newNode() }

                fun mapNode(compressedNode: CompressedPointsToGraph.Node): Pair<DataFlowIR.Node?, PointsToGraphNode?> {
                    val (arg, rootNode) = when (val kind = compressedNode.kind) {
                        CompressedPointsToGraph.NodeKind.Return -> arguments.last() to nodes[arguments.last()]
                        is CompressedPointsToGraph.NodeKind.Param -> arguments[kind.index] to nodes[arguments[kind.index]]
                        is CompressedPointsToGraph.NodeKind.Drain -> null to calleeDrains[kind.index]
                    }
                    if (rootNode == null)
                        return arg to rootNode
                    val path = compressedNode.path
                    var node: PointsToGraphNode = rootNode
                    for (field in path) {
                        node = when (field) {
                            returnsValueField -> node
                            else -> node.getFieldNode(field, this)
                        }
                    }
                    return arg to node
                }

                calleeEscapeAnalysisResult.escapes.forEach { escapingNode ->
                    val (arg, node) = mapNode(escapingNode)
                    if (node == null) {
                        context.log { "WARNING: There is no node ${nodeToString(arg!!)}" }
                        return@forEach
                    }
                    escapeOrigins += node
                    context.log { "Node ${escapingNode.debugString(arg?.let { nodeToString(it) })} escapes" }
                }

                calleeEscapeAnalysisResult.pointsTo.edges.forEach { edge ->
                    val (fromArg, fromNode) = mapNode(edge.from)
                    if (fromNode == null) {
                        context.log { "WARNING: There is no node ${nodeToString(fromArg!!)}" }
                        return@forEach
                    }
                    val (toArg, toNode) = mapNode(edge.to)
                    if (toNode == null) {
                        context.log { "WARNING: There is no node ${nodeToString(toArg!!)}" }
                        return@forEach
                    }
                    fromNode.addAssignmentEdge(toNode)

                    context.logMultiple {
                        +"Adding edge"
                        +"    FROM ${edge.from.debugString(fromArg?.let { nodeToString(it) })}"
                        +"    TO ${edge.to.debugString(toArg?.let { nodeToString(it) })}"
                    }
                }
            }

            fun buildClosure(): FunctionEscapeAnalysisResult {
                context.logMultiple {
                    +"BUILDING CLOSURE"
                    +"Return values:"
                    returnValues.forEach { +"    ${nodeToString(it)}" }
                }

                buildDrains()

                logDigraph(true)

                computeLifetimes()

                /*
                 * The next part determines the function's escape analysis result.
                 * Of course, the simplest way would be to just take the entire graph, but it might be big,
                 * and during call graph traversal these EA graphs will continue to grow (since they are
                 * being embedded at each call site). To overcome this, the graph must be reduced.
                 * Let us call nodes that will be part of the result "interesting", and, obviously,
                 * "interesting drains" - drains that are going to be in the result.
                 */
                val (numberOfDrains, nodeIds) = paintInterestingNodes()

                logDigraph(true, { nodeIds[it] != null }, { nodeIds[it].toString() })

                // TODO: Remove redundant edges.
                val compressedEdges = mutableListOf<CompressedPointsToGraph.Edge>()
                val escapingNodes = mutableListOf<CompressedPointsToGraph.Node>()
                for (from in allNodes) {
                    val fromCompressedNode = nodeIds[from] ?: continue
                    if (from in escapeOrigins)
                        escapingNodes += fromCompressedNode
                    for (edge in from.edges) {
                        val toCompressedNode = nodeIds[edge.node] ?: continue
                        when (edge) {
                            is PointsToGraphEdge.Assignment ->
                                compressedEdges += CompressedPointsToGraph.Edge(fromCompressedNode, toCompressedNode)

                            is PointsToGraphEdge.Field -> {
                                val next = fromCompressedNode.goto(edge.field)
                                if (next != toCompressedNode) // Skip loops.
                                    compressedEdges += CompressedPointsToGraph.Edge(next, toCompressedNode)
                            }
                        }
                    }
                }

                return FunctionEscapeAnalysisResult(
                        numberOfDrains,
                        CompressedPointsToGraph(compressedEdges.toTypedArray()),
                        escapingNodes.toTypedArray()
                )
            }

            private fun buildDrains() {
                // TODO: This is actually conservative. If a field is being read of some node,
                // then here it is assumed that it might also be being read from any node reachable
                // by assignment edges considering them undirected. But in reality it is enough to just
                // merge two sets: reachable by assignment edges and reachable by reversed assignment edges.
                // But, there will be a downside - drains will have to be created for each field access,
                // thus increasing the graph size significantly.
                val visited = mutableSetOf<PointsToGraphNode>()
                val drains = mutableListOf<PointsToGraphNode>()
                val createdDrains = mutableSetOf<PointsToGraphNode>()
                // Create drains.
                for (node in allNodes.toTypedArray() /* Copy as [allNodes] might change inside */) {
                    if (node in visited) continue
                    val component = mutableListOf<PointsToGraphNode>()
                    buildComponent(node, visited, component)
                    val drain = trySelectDrain(component)?.also { it.drain = it }
                            ?: newDrain().also { createdDrains += it }
                    drains += drain
                    component.forEach {
                        if (it == drain) return@forEach
                        it.drain = drain
                        val assignmentEdges = mutableListOf<PointsToGraphEdge>()
                        for (edge in it.edges) {
                            if (edge is PointsToGraphEdge.Assignment)
                                assignmentEdges += edge
                            else
                                drain.edges += edge
                        }
                        it.edges.clear()
                        it.edges += assignmentEdges
                    }
                }

                fun PointsToGraphNode.flipTo(otherDrain: PointsToGraphNode) {
                    require(isDrain)
                    require(otherDrain.isDrain)
                    drain = otherDrain
                    otherDrain.edges += edges
                    edges.clear()
                }

                // Merge the components multi-edges are pointing at.
                // TODO: This looks very similar to the system of disjoint sets algorithm.
                while (true) {
                    val toMerge = mutableListOf<Pair<PointsToGraphNode, PointsToGraphNode>>()
                    for (drain in drains) {
                        val fields = drain.edges.groupBy { edge ->
                            (edge as? PointsToGraphEdge.Field)?.field
                                    ?: error("A drain cannot have outgoing assignment edges")
                        }
                        for (nodes in fields.values) {
                            if (nodes.size == 1) continue
                            for (i in nodes.indices) {
                                val firstNode = nodes[i].node
                                val secondNode = if (i == nodes.size - 1) nodes[0].node else nodes[i + 1].node
                                if (firstNode.actualDrain != secondNode.actualDrain)
                                    toMerge += Pair(firstNode, secondNode)
                            }
                        }
                    }
                    if (toMerge.isEmpty()) break
                    val possibleDrains = mutableListOf<PointsToGraphNode>()
                    for ((first, second) in toMerge) {
                        // Merge components: try to flip one drain to the other if possible,
                        // otherwise just create a new one.

                        val firstDrain = first.actualDrain
                        val secondDrain = second.actualDrain
                        when {
                            firstDrain == secondDrain -> continue

                            firstDrain in createdDrains -> {
                                secondDrain.flipTo(firstDrain)
                                possibleDrains += firstDrain
                            }

                            secondDrain in createdDrains -> {
                                firstDrain.flipTo(secondDrain)
                                possibleDrains += secondDrain
                            }

                            else -> {
                                // Create a new drain in order to not create false constraints.
                                val newDrain = newDrain().also { createdDrains += it }
                                firstDrain.flipTo(newDrain)
                                secondDrain.flipTo(newDrain)
                                possibleDrains += newDrain
                            }
                        }
                    }
                    drains.clear()
                    possibleDrains.filterTo(drains) { it.isDrain }
                }

                // Compute current drains.
                drains.clear()
                allNodes.filterTo(drains) { it.isActualDrain }

                // A validation.
                for (drain in drains) {
                    val fields = mutableMapOf<DataFlowIR.Field, PointsToGraphNode>()
                    for (edge in drain.edges) {
                        val field = (edge as? PointsToGraphEdge.Field)?.field
                                ?: error("A drain cannot have outgoing assignment edges")
                        val node = edge.node.actualDrain
                        fields.getOrPut(field) { node }.also {
                            if (it != node) error("Drains have not been built correctly")
                        }
                    }
                }

                // Coalesce multi-edges.
                for (drain in drains) {
                    val actualDrain = drain.actualDrain
                    val fields = actualDrain.edges.groupBy { edge ->
                        (edge as? PointsToGraphEdge.Field)?.field
                                ?: error("A drain cannot have outgoing assignment edges")
                    }
                    actualDrain.edges.clear()
                    for (nodes in fields.values) {
                        if (nodes.size == 1) {
                            actualDrain.edges += nodes[0]
                            continue
                        }
                        // All nodes in [nodes] must be connected to each other, but a drain, by definition,
                        // cannot have outgoing assignment edges, thus a new drain must be created here.
                        nodes.atMostOne { it.node.isActualDrain }
                                ?.node?.actualDrain?.flipTo(newDrain())

                        for (i in nodes.indices) {
                            val firstNode = nodes[i].node
                            val secondNode = if (i == nodes.size - 1) nodes[0].node else nodes[i + 1].node
                            firstNode.addAssignmentEdge(secondNode)
                        }
                        // Can pick any.
                        actualDrain.edges += nodes[0]
                    }
                }

                // Make sure every node within a component points to the component's drain.
                for (node in allNodes) {
                    val drain = node.actualDrain
                    node.drain = drain
                    if (node != drain)
                        node.addAssignmentEdge(drain)
                }
            }

            // Drains, other than interesting, can be safely omitted from the result.
            private fun findInterestingDrains(parameters: Array<PointsToGraphNode>): Set<PointsToGraphNode> {
                // Starting with all reachable from the parameters.
                val interestingDrains = mutableSetOf<PointsToGraphNode>()
                for (param in parameters) {
                    val drain = param.drain
                    if (drain !in interestingDrains)
                        findReachableDrains(drain, interestingDrains)
                }

                // Then iteratively remove all drains forming kind of a "cactus"
                // (picking a leaf drain with only one incoming edge at a time).
                // They can be removed because they don't add any relations between the parameters.
                val reversedEdges = interestingDrains.associateWith {
                    mutableListOf<PointsToGraphNode>()
                }
                val edgesCount = mutableMapOf<PointsToGraphNode, Int>()
                val leaves = mutableListOf<PointsToGraphNode>()
                for (drain in interestingDrains) {
                    var count = 0
                    for (edge in drain.edges) {
                        val nextDrain = edge.node.drain
                        reversedEdges[nextDrain]!! += drain
                        if (nextDrain in interestingDrains)
                            ++count
                    }
                    edgesCount[drain] = count
                    if (count == 0)
                        leaves.push(drain)
                }
                val parameterDrains = parameters.map { it.drain }.toSet()
                while (leaves.isNotEmpty()) {
                    val drain = leaves.pop()
                    val incomingEdges = reversedEdges[drain]!!
                    if (incomingEdges.isEmpty()) {
                        if (drain !in parameterDrains)
                            error("A drain with no incoming edges")
                        if (!parameters.any { it.isDrain && escapes(it) })
                            interestingDrains.remove(drain)
                        continue
                    }
                    if (drain in parameterDrains)
                        continue
                    if (incomingEdges.size == 1
                            && (escapes(incomingEdges[0]) || !escapes(drain))
                    ) {
                        interestingDrains.remove(drain)
                        val prevDrain = incomingEdges[0]
                        val count = edgesCount[prevDrain]!! - 1
                        edgesCount[prevDrain] = count
                        if (count == 0)
                            leaves.push(prevDrain)
                    }
                }
                return interestingDrains
            }

            private fun getParameterNodes(): Array<PointsToGraphNode> {
                // Put a dummyNode in order to not bother with nullability. Then rewrite it with actual values.
                val dummyNode = returnsNode // Anything will do.
                val parameters = Array(functionSymbol.parameters.size + 1) { dummyNode }

                // Parameters are declared in the root scope.
                function.body.rootScope.nodes
                        .filterIsInstance<DataFlowIR.Node.Parameter>()
                        .forEach {
                            if (parameters[it.index] != dummyNode)
                                error("Two parameters with the same index ${it.index}: $it, ${parameters[it.index].node}")
                            parameters[it.index] = nodes[it]!!
                        }
                parameters[functionSymbol.parameters.size] = returnsNode

                return parameters
            }

            private fun paintInterestingNodes(): Pair<Int, Map<PointsToGraphNode, CompressedPointsToGraph.Node>> {
                var drainsCount = 0
                val drainFactory = { CompressedPointsToGraph.Node.drain(drainsCount++) }

                val parameters = getParameterNodes()
                val interestingDrains = findInterestingDrains(parameters)
                val nodeIds = paintNodes(parameters, interestingDrains, drainFactory)
                buildComponentsClosures(nodeIds)
                handleNotTakenEscapeOrigins(nodeIds, drainFactory)
                restoreOptimizedAwayDrainsIfNeeded(interestingDrains, nodeIds, drainFactory)

                return Pair(drainsCount, nodeIds)
            }

            private fun handleNotTakenEscapeOrigins(
                    nodeIds: MutableMap<PointsToGraphNode, CompressedPointsToGraph.Node>,
                    drainFactory: () -> CompressedPointsToGraph.Node
            ) {
                // We've marked reachable nodes from the parameters, also taking some of the escape origins.
                // But there might be some escape origins that are not taken, yet referencing some of the
                // marked nodes. Do the following: find all escaping nodes only taking marked escape origins
                // into account, compare the result with all escaping nodes. Now for each non-marked escape origin
                // find nodes escaping because of it, take those who aren't escaping through the marked origins,
                // and add an additional node, pointing at those and mark it as an escape origin.
                val reachableFromTakenEscapeOrigins = mutableSetOf<PointsToGraphNode>()
                val referencingTakenEscapeOrigins = mutableSetOf<PointsToGraphNode>()
                val reachableFromNotTakenEscapeOrigins = mutableSetOf<PointsToGraphNode>()
                val referencingNotTakenEscapeOrigins = mutableSetOf<PointsToGraphNode>()
                val reachableFringeFromNotTakenEscapeOrigins = mutableSetOf<PointsToGraphNode>()
                val fringeReferencingNotTakenEscapeOrigins = mutableSetOf<PointsToGraphNode>()
                for (escapeOrigin in escapeOrigins) {
                    if (nodeIds[escapeOrigin] == null) {
                        if (escapeOrigin !in reachableFromNotTakenEscapeOrigins)
                            findReachableFringe(escapeOrigin, reachableFromNotTakenEscapeOrigins,
                                    reachableFringeFromNotTakenEscapeOrigins, nodeIds)
                        if (escapeOrigin !in referencingNotTakenEscapeOrigins)
                            findReferencingFringe(escapeOrigin, referencingNotTakenEscapeOrigins,
                                    fringeReferencingNotTakenEscapeOrigins, nodeIds)
                    } else {
                        if (escapeOrigin !in reachableFromTakenEscapeOrigins)
                            findReachable(escapeOrigin, reachableFromTakenEscapeOrigins, false, null)
                        if (escapeOrigin !in referencingTakenEscapeOrigins)
                            findReferencing(escapeOrigin, referencingTakenEscapeOrigins)
                    }
                }

                fun addAdditionalEscapeOrigins(escapingNodes: List<PointsToGraphNode>, direction: EdgeDirection) {
                    escapingNodes
                            .groupBy { it.drain }
                            .forEach { (drain, nodes) ->
                                val tempNode = newNode()
                                nodeIds[tempNode] = drainFactory()
                                tempNode.drain = drain
                                tempNode.addAssignmentEdge(drain)
                                escapeOrigins += tempNode
                                for (node in nodes)
                                    when (direction) {
                                        EdgeDirection.FORWARD -> tempNode.addAssignmentEdge(node)
                                        EdgeDirection.BACKWARD -> node.addAssignmentEdge(tempNode)
                                    }
                            }
                }

                addAdditionalEscapeOrigins(
                        reachableFringeFromNotTakenEscapeOrigins
                                .filterNot { it in reachableFromTakenEscapeOrigins },
                        EdgeDirection.FORWARD
                )
                addAdditionalEscapeOrigins(
                        fringeReferencingNotTakenEscapeOrigins
                                .filterNot { it in referencingTakenEscapeOrigins },
                        EdgeDirection.BACKWARD
                )
            }

            private fun restoreOptimizedAwayDrainsIfNeeded(
                    interestingDrains: Set<PointsToGraphNode>,
                    nodeIds: MutableMap<PointsToGraphNode, CompressedPointsToGraph.Node>,
                    drainFactory: () -> CompressedPointsToGraph.Node
            ) {
                // Here we try to find this subgraph within one component: [v -> d; w -> d; v !-> w; w !-> v].
                // This is needed because components are built with edges being considered undirected, so
                // this implicit connection between [v] and [w] may be needed. Note, however, that the
                // opposite subgraph: [d -> v; d -> w; v !-> w; w !-> v] is not interesting, because [d]
                // can't hold both values simultaneously, but two references can hold the same value
                // at the same time, that's the difference.
                // For concrete example see [codegen/escapeAnalysis/test10.kt].
                // Note: it is possible to search not for all such nodes, but for drains only, here's why:
                // Assume a node [v] which is referenced from the two fixed nodes is found, then consider
                // the [v]'s drain, by construction all drains are reachable from all nodes within the corresponding
                // component, including [v]; this implies that the drain also is referenced from these two nodes,
                // and therefore it is possible to check only drains rather than all nodes.
                val connectedNodes = mutableSetOf<Pair<PointsToGraphNode, PointsToGraphNode>>()
                allNodes.filter { nodeIds[it] != null && nodeIds[it.drain] == null /* The drain has been optimized away */ }
                        .forEach { node ->
                            val referencingNodes = findReferencing(node).filter { nodeIds[it] != null }
                            for (i in referencingNodes.indices)
                                for (j in i + 1 until referencingNodes.size) {
                                    val firstNode = referencingNodes[i]
                                    val secondNode = referencingNodes[j]
                                    connectedNodes.add(Pair(firstNode, secondNode))
                                    connectedNodes.add(Pair(secondNode, firstNode))
                                }
                        }

                interestingDrains
                        .filter { nodeIds[it] == null } // Was optimized away.
                        .forEach { drain ->
                            val referencingNodes = findReferencing(drain).filter { nodeIds[it] != null }
                            if (escapes(drain) && referencingNodes.all { !escapes(it) }) {
                                nodeIds[drain] = drainFactory()
                                escapeOrigins += drain
                            } else {
                                var needDrain = false
                                outerLoop@ for (i in referencingNodes.indices)
                                    for (j in i + 1 until referencingNodes.size) {
                                        val firstNode = referencingNodes[i]
                                        val secondNode = referencingNodes[j]
                                        val pair = Pair(firstNode, secondNode)
                                        if (pair !in connectedNodes) {
                                            needDrain = true
                                            break@outerLoop
                                        }
                                    }
                                if (needDrain)
                                    nodeIds[drain] = drainFactory()
                            }
                        }
            }

            private fun findReferencing(node: PointsToGraphNode, visited: MutableSet<PointsToGraphNode>) {
                visited += node
                for (edge in node.reversedEdges) {
                    val nextNode = edge.node
                    if (nextNode !in visited)
                        findReferencing(nextNode, visited)
                }
            }

            private fun findReferencing(node: PointsToGraphNode): Set<PointsToGraphNode> {
                val visited = mutableSetOf<PointsToGraphNode>()
                findReferencing(node, visited)
                return visited
            }

            private fun trySelectDrain(component: MutableList<PointsToGraphNode>) =
                    component.firstOrNull { node ->
                        if (node.edges.any { it is PointsToGraphEdge.Assignment })
                            false
                        else
                            findReferencing(node).size == component.size
                    }

            private fun buildComponent(
                    node: PointsToGraphNode,
                    visited: MutableSet<PointsToGraphNode>,
                    component: MutableList<PointsToGraphNode>
            ) {
                visited += node
                component += node
                for (edge in node.edges) {
                    if (edge is PointsToGraphEdge.Assignment && edge.node !in visited)
                        buildComponent(edge.node, visited, component)
                }
                for (edge in node.reversedEdges) {
                    if (edge.node !in visited)
                        buildComponent(edge.node, visited, component)
                }
            }

            private fun findReachable(node: PointsToGraphNode, visited: MutableSet<PointsToGraphNode>,
                                      assignmentOnly: Boolean,
                                      nodeIds: MutableMap<PointsToGraphNode, CompressedPointsToGraph.Node>?) {
                visited += node
                node.edges.forEach {
                    val next = it.node
                    if ((it is PointsToGraphEdge.Assignment || !assignmentOnly)
                            && next !in visited && nodeIds?.containsKey(next) != false)
                        findReachable(next, visited, assignmentOnly, nodeIds)
                }
            }

            private fun findFringe(node: PointsToGraphNode, visited: MutableSet<PointsToGraphNode>,
                                   fringe: MutableSet<PointsToGraphNode>, direction: EdgeDirection,
                                   nodeIds: MutableMap<PointsToGraphNode, CompressedPointsToGraph.Node>) {
                visited += node
                if (nodeIds[node] != null) {
                    fringe += node
                    return
                }
                val edges = when (direction) {
                    EdgeDirection.FORWARD -> node.edges
                    EdgeDirection.BACKWARD -> node.reversedEdges
                }
                for (edge in edges) {
                    val next = edge.node
                    if (next !in visited)
                        findFringe(next, visited, fringe, direction, nodeIds)
                }
            }

            private fun findReachableFringe(
                    node: PointsToGraphNode, visited: MutableSet<PointsToGraphNode>,
                    fringe: MutableSet<PointsToGraphNode>,
                    nodeIds: MutableMap<PointsToGraphNode, CompressedPointsToGraph.Node>
            ) = findFringe(node, visited, fringe, EdgeDirection.FORWARD, nodeIds)

            private fun findReferencingFringe(
                    node: PointsToGraphNode, visited: MutableSet<PointsToGraphNode>,
                    fringe: MutableSet<PointsToGraphNode>,
                    nodeIds: MutableMap<PointsToGraphNode, CompressedPointsToGraph.Node>
            ) = findFringe(node, visited, fringe, EdgeDirection.BACKWARD, nodeIds)

            private fun buildComponentsClosures(nodeIds: MutableMap<PointsToGraphNode, CompressedPointsToGraph.Node>) {
                for (node in allNodes) {
                    if (node !in nodeIds) continue
                    val visited = mutableSetOf<PointsToGraphNode>()
                    findReachable(node, visited, true, null)
                    val visitedInInterestingSubgraph = mutableSetOf<PointsToGraphNode>()
                    findReachable(node, visitedInInterestingSubgraph, true, nodeIds)
                    visited.removeAll(visitedInInterestingSubgraph)
                    for (reachable in visited)
                        if (reachable in nodeIds)
                            node.addAssignmentEdge(reachable)
                }
            }

            private fun propagateLifetimes() {
                val visited = mutableSetOf<PointsToGraphNode>()

                fun propagate(node: PointsToGraphNode) {
                    visited += node
                    val depth = node.depth
                    for (edge in node.edges) {
                        val nextNode = edge.node
                        if (nextNode !in visited && nextNode.depth >= depth) {
                            nextNode.depth = depth
                            propagate(nextNode)
                        }
                    }
                }

                for (node in allNodes.sortedBy { it.depth }) {
                    if (node !in visited)
                        propagate(node)
                }
            }

            private fun propagateEscapeOrigin(node: PointsToGraphNode) {
                if (node !in reachableFromEscapeOrigins)
                    findReachable(node, reachableFromEscapeOrigins, false, null)
                if (node !in referencingEscapeOrigins)
                    findReferencing(node, referencingEscapeOrigins)
            }

            private fun computeLifetimes() {
                propagateLifetimes()

                escapeOrigins.forEach { propagateEscapeOrigin(it) }

                // TODO: To a setting?
                val allowedToAlloc = 65536
                val stackArrayCandidates = mutableListOf<ArrayStaticAllocation>()
                for ((node, ptgNode) in nodes) {
                    if (node.ir == null) continue

                    val computedLifetime = lifetimeOf(node)
                    var lifetime = computedLifetime

                    if (lifetime != Lifetime.STACK) {
                        // TODO: Support other lifetimes - requires arenas.
                        lifetime = Lifetime.GLOBAL
                    }

                    if (lifetime == Lifetime.STACK && node is DataFlowIR.Node.AllocArray) {
                        val constructedType = node.type
                        constructedType.irClass?.let { irClass ->
                            val itemSize = arrayItemSizeOf(irClass)
                            if (itemSize != null) {
                                val sizeArgument = node.size.node
                                val arrayLength = arrayLengthOf(sizeArgument)?.takeIf { it >= 0 }
                                val arraySizeInBytes = arraySizeInBytes(itemSize, arrayLength ?: Int.MAX_VALUE)
                                if (arraySizeInBytes <= allowedToAlloc) {
                                    stackArrayCandidates += ArrayStaticAllocation(ptgNode, irClass, arrayLength!!, arraySizeInBytes.toInt())
                                } else {
                                    // Can be placed into the local arena.
                                    // TODO. Support Lifetime.LOCAL
                                    lifetime = Lifetime.GLOBAL
                                }
                            }
                        }
                    }

                    if (lifetime != computedLifetime) {
                        if (propagateExiledToHeapObjects && node is DataFlowIR.Node.Alloc) {
                            context.log { "Forcing node ${nodeToString(node)} to escape" }
                            escapeOrigins += ptgNode
                            propagateEscapeOrigin(ptgNode)
                        } else {
                            ptgNode.forcedLifetime = lifetime
                        }
                    }
                }

                stackArrayCandidates.sortBy { it.sizeInBytes }
                var remainedToAlloc = allowedToAlloc
                for ((ptgNode, irClass, length, sizeInBytes) in stackArrayCandidates) {
                    if (lifetimeOf(ptgNode) != Lifetime.STACK) continue
                    if (sizeInBytes <= remainedToAlloc) {
                        remainedToAlloc -= sizeInBytes
                        ptgNode.forcedLifetime = Lifetime.STACK_ARRAY(length)
                    } else {
                        remainedToAlloc = 0
                        // Do not exile primitive arrays - they ain't reference no object.
                        if (irClass.symbol == symbols.array && propagateExiledToHeapObjects) {
                            context.log { "Forcing node ${nodeToString(ptgNode.node!!)} to escape" }
                            escapeOrigins += ptgNode
                            propagateEscapeOrigin(ptgNode)
                        } else {
                            ptgNode.forcedLifetime = Lifetime.GLOBAL // TODO: Change to LOCAL when supported.
                        }
                    }
                }
            }

            private fun findReachableDrains(drain: PointsToGraphNode, visitedDrains: MutableSet<PointsToGraphNode>) {
                visitedDrains += drain
                for (edge in drain.edges) {
                    if (edge is PointsToGraphEdge.Assignment)
                        error("A drain cannot have outgoing assignment edges")
                    val nextDrain = edge.node.drain
                    if (nextDrain !in visitedDrains)
                        findReachableDrains(nextDrain, visitedDrains)
                }
            }

            private fun paintNodes(
                    parameters: Array<PointsToGraphNode>,
                    interestingDrains: Set<PointsToGraphNode>,
                    drainFactory: () -> CompressedPointsToGraph.Node
            ): MutableMap<PointsToGraphNode, CompressedPointsToGraph.Node> {
                val nodeIds = mutableMapOf<PointsToGraphNode, CompressedPointsToGraph.Node>()

                for (index in parameters.indices)
                    nodeIds[parameters[index]] = CompressedPointsToGraph.Node.parameter(index, parameters.size)

                val standAloneDrains = interestingDrains.toMutableSet()
                for (drain in interestingDrains)
                    for (edge in drain.edges) {
                        val node = edge.node
                        if (node.isDrain && node != drain /* Skip loops */)
                            standAloneDrains.remove(node)
                    }
                for (drain in standAloneDrains) {
                    if (nodeIds[drain] == null
                            // A little optimization - skip leaf drains.
                            && drain.edges.any { it.node.drain in interestingDrains })
                        nodeIds[drain] = drainFactory()
                }

                var front = nodeIds.keys.toMutableList()
                do {
                    while (front.isNotEmpty()) {
                        val nextFront = mutableListOf<PointsToGraphNode>()
                        for (node in front) {
                            val nodeId = nodeIds[node]!!
                            node.edges.filterIsInstance<PointsToGraphEdge.Field>().forEach { edge ->
                                val field = edge.field
                                val nextNode = edge.node
                                if (nextNode.drain in interestingDrains && nextNode != node /* Skip loops */) {
                                    val nextNodeId = nodeId.goto(field)
                                    if (nodeIds[nextNode] == null) {
                                        nodeIds[nextNode] = nextNodeId
                                        if (nextNode.isDrain)
                                            nextFront += nextNode
                                    }
                                }
                            }
                        }
                        front = nextFront
                    }

                    // Find unpainted drain.
                    for (drain in interestingDrains) {
                        if (nodeIds[drain] == null
                                // A little optimization - skip leaf drains.
                                && drain.edges.any { it.node.drain in interestingDrains }
                        ) {
                            nodeIds[drain] = drainFactory()
                            front += drain
                            break
                        }
                    }
                } while (front.isNotEmpty()) // Loop until all drains have been painted.

                return nodeIds
            }
        }
    }

    fun computeLifetimes(
            context: Context,
            generationState: NativeGenerationState,
            moduleDFG: ModuleDFG,
            callGraph: CallGraph,
            lifetimes: MutableMap<IrElement, Lifetime>
    ) {
        assert(lifetimes.isEmpty())

        try {
            InterproceduralAnalysis(context, generationState, callGraph,
                    moduleDFG, lifetimes,
                    // The GC must be careful not to scan exiled objects, that have already became dead,
                    // as they may reference other already destroyed stack-allocated objects.
                    // TODO somehow tag these object, so that GC could handle them properly.
                    propagateExiledToHeapObjects = context.config.gc == GC.CONCURRENT_MARK_AND_SWEEP
            ).analyze()
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