/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.DirectedGraph
import org.jetbrains.kotlin.backend.konan.DirectedGraphNode
import org.jetbrains.kotlin.backend.konan.NativeBackendContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class CallGraphNode(val graph: CallGraph, val symbol: DataFlowIR.FunctionSymbol.Declared)
    : DirectedGraphNode<DataFlowIR.FunctionSymbol.Declared> {

    override val key get() = symbol

    override val directEdges: List<DataFlowIR.FunctionSymbol.Declared> by lazy {
        buildSet {
            for (callSite in callSites) {
                if (callSite !is CallSite.Static) continue
                callSite.forEachCallee { callee ->
                    if (callee is DataFlowIR.FunctionSymbol.Declared && graph.directEdges.containsKey(callee))
                        add(callee)
                }
            }
        }.toList()
    }

    override val reversedEdges: List<DataFlowIR.FunctionSymbol.Declared> by lazy {
        graph.reversedEdges[symbol]!!
    }

    sealed class CallSite(
            val call: DataFlowIR.Node.Call,
            val node: DataFlowIR.Node,
    ) {
        class Virtual(call: DataFlowIR.Node.Call, node: DataFlowIR.Node, val callee: DataFlowIR.FunctionSymbol) : CallSite(call, node)

        sealed class Static(call: DataFlowIR.Node.Call, node: DataFlowIR.Node) : CallSite(call, node) {
            // Most sites have just one target. Store it directly instead of allocating a singleton list.
            class Single(call: DataFlowIR.Node.Call, node: DataFlowIR.Node, val callee: DataFlowIR.FunctionSymbol) : Static(call, node)

            class Multiple(
                    call: DataFlowIR.Node.Call, node: DataFlowIR.Node, val callees: List<DataFlowIR.FunctionSymbol>,
            ) : Static(call, node)

            val calleeCount: Int
                get() = when (this) {
                    is Single -> 1
                    is Multiple -> callees.size
                }

            inline fun forEachCallee(block: (DataFlowIR.FunctionSymbol) -> Unit) = when (this) {
                is Single -> block(callee)
                is Multiple -> for (i in callees.indices) block(callees[i])
            }

            companion object {
                fun create(call: DataFlowIR.Node.Call, node: DataFlowIR.Node, callees: List<DataFlowIR.FunctionSymbol>): Static {
                    require(callees.isNotEmpty())
                    return if (callees.size == 1) Single(call, node, callees[0]) else Multiple(call, node, callees)
                }
            }
        }
    }

    val callSites = mutableListOf<CallSite>()
}

internal class CallGraph(
        val directEdges: Map<DataFlowIR.FunctionSymbol.Declared, CallGraphNode>,
        val reversedEdges: MutableMap<DataFlowIR.FunctionSymbol.Declared, MutableList<DataFlowIR.FunctionSymbol.Declared>>,
        val rootExternalFunctions: List<DataFlowIR.FunctionSymbol>,
        val rootSet: Set<DataFlowIR.FunctionSymbol.Declared>,
) : DirectedGraph<DataFlowIR.FunctionSymbol.Declared, CallGraphNode> {
    override val nodes get() = directEdges.values

    override fun get(key: DataFlowIR.FunctionSymbol.Declared) = directEdges[key]!!

    fun addEdge(caller: DataFlowIR.FunctionSymbol.Declared, callSite: CallGraphNode.CallSite) {
        directEdges[caller]!!.callSites += callSite
    }

    fun addReversedEdge(caller: DataFlowIR.FunctionSymbol.Declared, callee: DataFlowIR.FunctionSymbol.Declared) {
        val callers = reversedEdges.getOrPut(callee) { mutableListOf() }
        // All edges from a single caller are added consecutively (while handling that caller's call sites),
        // so comparing with the last element is enough to keep the list free of duplicates.
        if (callers.lastOrNull() != caller)
            callers.add(caller)
    }
}

/**
 * Buffers targets only while a call can still be unfolded. Once it becomes opaque, all targets
 * are registered as roots in their original order, and the buffer is cleared.
 */
private class CallGraphCalleeCollector(private val unfoldFactor: Int) {
    val callees: List<DataFlowIR.FunctionSymbol>
            field = mutableListOf<DataFlowIR.FunctionSymbol>()
    var isOpaque: Boolean = unfoldFactor < 0
        private set

    inline fun add(callee: DataFlowIR.FunctionSymbol, handleRoot: (DataFlowIR.FunctionSymbol) -> Unit) {
        if (!isOpaque && callees.size < unfoldFactor) {
            callees.add(callee)
            return
        }
        if (!isOpaque) {
            isOpaque = true
            callees.forEach(handleRoot)
            callees.clear()
        }
        handleRoot(callee)
    }
}

/**
 * Only for calls without call-site-specific devirtualization results. Their targets depend solely on
 * the receiver's static type and dispatch slot. The hierarchy is immutable during call graph construction.
 * Keep this cache local to one builder: opaque targets must be registered in each new graph's root set.
 */
private class CallGraphHierarchyDispatchCache(
        private val typeHierarchy: DataFlowIR.TypeHierarchy,
        private val unfoldFactor: Int,
        private val handleRoot: (DataFlowIR.FunctionSymbol) -> Unit,
) {
    private data class Key(val receiverTypeIndex: Int, val interfaceId: Int, val slot: Int)

    private companion object {
        // Share one opaque marker, distinct from empty target lists.
        private val OPAQUE: List<DataFlowIR.FunctionSymbol> = ArrayList(0)
    }

    private val resolutions = HashMap<Key, List<DataFlowIR.FunctionSymbol>>()

    fun resolve(call: DataFlowIR.Node.VirtualCall) = resolutions.getOrPut(
            key = when (call) {
                is DataFlowIR.Node.VtableCall ->
                    Key(call.receiverType.index, 0 /* Invalid interface ID */, call.calleeVtableIndex)
                is DataFlowIR.Node.ItableCall ->
                    Key(call.receiverType.index, call.interfaceId, call.calleeItableIndex)
            }
    ) {
        val collector = CallGraphCalleeCollector(unfoldFactor)
        typeHierarchy.inheritorsOf(call.receiverType).forEachBit {
            // TODO: Unconservative way - when we can use it?
            //.filter { devirtualizationAnalysisResult.instantiatingClasses.contains(it) }
            val receiverType = typeHierarchy.allTypes[it]
            if (receiverType.isAbstract) return@forEachBit
            val actualCallee = when (call) {
                is DataFlowIR.Node.VtableCall -> receiverType.vtable[call.calleeVtableIndex]
                is DataFlowIR.Node.ItableCall -> receiverType.itable[call.interfaceId]!![call.calleeItableIndex]
            }
            collector.add(actualCallee, handleRoot)
        }
        if (collector.isOpaque) OPAQUE else collector.callees
    }.takeUnless { it === OPAQUE }
}

@TestOnly
internal fun createDispatchResolverForTests(
        typeHierarchy: DataFlowIR.TypeHierarchy,
        unfoldFactor: Int,
        handleRoot: (DataFlowIR.FunctionSymbol) -> Unit,
): (DataFlowIR.Node.VirtualCall) -> List<DataFlowIR.FunctionSymbol>? {
    val cache = CallGraphHierarchyDispatchCache(typeHierarchy, unfoldFactor, handleRoot)
    return cache::resolve
}

internal class CallGraphBuilder(
        val context: NativeBackendContext,
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
        val devirtualizedCallSitesUnfoldFactor: Int,
        val nonDevirtualizedCallSitesUnfoldFactor: Int,
) {
    private val directEdges = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, CallGraphNode>()
    private val reversedEdges = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, MutableList<DataFlowIR.FunctionSymbol.Declared>>()
    private val externalRootFunctions = mutableListOf<DataFlowIR.FunctionSymbol>()
    private val wholeRootSet = mutableSetOf<DataFlowIR.FunctionSymbol.Declared>()
    private val callGraph = CallGraph(directEdges, reversedEdges, externalRootFunctions, wholeRootSet)

    private val functionStack = FunctionStack()
    private val hierarchyDispatchCache = lazy {
        CallGraphHierarchyDispatchCache(
                typeHierarchy = moduleDFG.symbolTable.typeHierarchy,
                unfoldFactor = nonDevirtualizedCallSitesUnfoldFactor,
        ) { handleRoot(it) }
    }

    private inner class FunctionStack {
        private val stack = mutableListOf<DataFlowIR.Function>()

        fun push(caller: DataFlowIR.FunctionSymbol.Declared?, callee: DataFlowIR.Function) {
            val calleeSymbol = callee.symbol as DataFlowIR.FunctionSymbol.Declared
            // The reversed edge is recorded right away, so the callee only needs to be pushed
            // if it hasn't been included in the Call Graph yet.
            if (caller != null)
                callGraph.addReversedEdge(caller, calleeSymbol)
            if (!directEdges.containsKey(calleeSymbol))
                stack.push(callee)
        }

        fun process() {
            while (stack.isNotEmpty()) {
                val calleeFunction = stack.pop()
                val callee = calleeFunction.symbol as DataFlowIR.FunctionSymbol.Declared

                // If the same callee was put on the stack multiple times in one go, and at least one was popped and included
                // in the Call Graph, any op on it (current and future) is a no-op. Skip such callees.
                if (directEdges.containsKey(callee)) continue
                addNode(callee)
                handleFunction(callee, calleeFunction)
            }
        }
    }

    fun build(): CallGraph {
        val rootSet = DevirtualizationAnalysis.computeRootSet(context, irModule, moduleDFG)
        rootSet.forEach { handleRoot(it) }

        functionStack.process()
        return callGraph
    }

    private fun addNode(symbol: DataFlowIR.FunctionSymbol.Declared) {
        directEdges[symbol] = CallGraphNode(callGraph, symbol)
        // Reversed edges to this node might have been recorded before the node itself was added.
        val _ = reversedEdges.getOrPut(symbol) { mutableListOf() }
    }

    private inline fun DataFlowIR.FunctionBody.forEachCallSite(block: (DataFlowIR.Node.Call, DataFlowIR.Node) -> Unit): Unit =
            forEachNonScopeNode { node ->
                when (node) {
                    is DataFlowIR.Node.Call -> block(node, node)

                    is DataFlowIR.Node.Singleton ->
                        node.constructor?.let { constructor ->
                            val arguments = buildList {
                                add(DataFlowIR.Edge(node, null)) // this.
                                node.arguments?.let { addAll(it) }
                            }
                            block(DataFlowIR.Node.Call(constructor, arguments, node.type, null), node)
                        }

                    is DataFlowIR.Node.ArrayRead ->
                        block(DataFlowIR.Node.Call(
                                callee = node.callee,
                                arguments = listOf(node.array, node.index),
                                returnType = node.type,
                                irCallSite = null),
                                node
                        )

                    is DataFlowIR.Node.ArrayWrite ->
                        block(DataFlowIR.Node.Call(
                                callee = node.callee,
                                arguments = listOf(node.array, node.index, node.value),
                                returnType = moduleDFG.symbolTable.mapType(context.irBuiltIns.unitType),
                                irCallSite = null),
                                node
                        )

                    else -> { }
                }
            }

    private fun staticCall(
            caller: DataFlowIR.FunctionSymbol.Declared,
            call: DataFlowIR.Node.Call,
            node: DataFlowIR.Node,
            callees: List<DataFlowIR.FunctionSymbol>,
    ) {
        if (callees.isEmpty()) return
        staticCall(caller, CallGraphNode.CallSite.Static.create(call, node, callees))
    }

    private fun staticCall(caller: DataFlowIR.FunctionSymbol.Declared, callSite: CallGraphNode.CallSite.Static) {
        callGraph.addEdge(caller, callSite)
        callSite.forEachCallee { callee ->
            val function = moduleDFG.functions[callee]
            if (function != null)
                functionStack.push(caller, function)
        }
    }

    private fun handleRoot(symbol: DataFlowIR.FunctionSymbol) {
        val function = moduleDFG.functions[symbol]
        if (function == null)
            externalRootFunctions.add(symbol)
        else {
            wholeRootSet.add(symbol as DataFlowIR.FunctionSymbol.Declared)
            functionStack.push(null, function)
        }
    }

    private fun handleFunction(symbol: DataFlowIR.FunctionSymbol.Declared, function: DataFlowIR.Function) {
        val body = function.body
        body.forEachCallSite { call, node ->
            val devirtualizedCallSite = (call as? DataFlowIR.Node.VirtualCall)?.irCallSite?.devirtualizedCallSite
            when {
                call !is DataFlowIR.Node.VirtualCall -> staticCall(symbol, CallGraphNode.CallSite.Static.Single(call, node, call.callee))

                devirtualizedCallSite != null -> {
                    if (devirtualizedCallSite.possibleCallees.size <= devirtualizedCallSitesUnfoldFactor) {
                        if (devirtualizedCallSite.possibleCallees.size == 1)
                            staticCall(symbol, CallGraphNode.CallSite.Static.Single(call, node, devirtualizedCallSite.possibleCallees[0].callee))
                        else
                            staticCall(symbol, call, node, devirtualizedCallSite.possibleCallees.map { it.callee })
                    } else {
                        val callSite = CallGraphNode.CallSite.Virtual(call, node, call.callee)
                        callGraph.addEdge(symbol, callSite)

                        devirtualizedCallSite.possibleCallees.forEach { handleRoot(it.callee) }
                    }

                }

                call.receiverType == DataFlowIR.Type.Virtual -> {
                    // Skip callsite. This can only be for invocations Any's methods on instances of ObjC classes.
                }

                else -> {
                    // Callsite has not been devirtualized - conservatively assume the worst:
                    // any inheritor of the receiver type is possible here.
                    val callees = hierarchyDispatchCache.value.resolve(call)
                    if (callees != null)
                        staticCall(symbol, call, node, callees)
                    else {
                        val callSite = CallGraphNode.CallSite.Virtual(call, node, call.callee)
                        callGraph.addEdge(symbol, callSite)
                    }
                }
            }
        }
    }
}
