/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

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
        graph.directEdges[symbol]!!.callSites
                .filterIsInstance<CallSite.Static>()
                .flatMap { it.callees }
                .filterIsInstance<DataFlowIR.FunctionSymbol.Declared>()
                .filter { graph.directEdges.containsKey(it) }
    }

    override val reversedEdges: List<DataFlowIR.FunctionSymbol.Declared> by lazy {
        graph.reversedEdges[symbol]!!
    }

    sealed class CallSite(
            val call: DataFlowIR.Node.Call,
            val node: DataFlowIR.Node,
    ) {
        class Virtual(call: DataFlowIR.Node.Call, node: DataFlowIR.Node, val callee: DataFlowIR.FunctionSymbol) : CallSite(call, node)

        // A call site unfolded to several possible callees (a devirtualized one, or a virtual one unfolded by the type hierarchy)
        // is represented as a single instance listing all of them in [callees].
        class Static(
                call: DataFlowIR.Node.Call, node: DataFlowIR.Node, val callees: List<DataFlowIR.FunctionSymbol>
        ) : CallSite(call, node)
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

internal class CallGraphBuilder(
        val context: NativeBackendContext,
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
        val devirtualizedCallSitesUnfoldFactor: Int,
        val nonDevirtualizedCallSitesUnfoldFactor: Int
) {
    private val directEdges = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, CallGraphNode>()
    private val reversedEdges = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, MutableList<DataFlowIR.FunctionSymbol.Declared>>()
    private val externalRootFunctions = mutableListOf<DataFlowIR.FunctionSymbol>()
    private val wholeRootSet = mutableSetOf<DataFlowIR.FunctionSymbol.Declared>()
    private val callGraph = CallGraph(directEdges, reversedEdges, externalRootFunctions, wholeRootSet)

    private val functionStack = FunctionStack()

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
        reversedEdges.getOrPut(symbol) { mutableListOf() }
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
        callGraph.addEdge(caller, CallGraphNode.CallSite.Static(call, node, callees))
        for (callee in callees) {
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
                call !is DataFlowIR.Node.VirtualCall -> staticCall(symbol, call, node, listOf(call.callee))

                devirtualizedCallSite != null -> {
                    if (devirtualizedCallSite.possibleCallees.size <= devirtualizedCallSitesUnfoldFactor)
                        staticCall(symbol, call, node, devirtualizedCallSite.possibleCallees.map { it.callee })
                    else {
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
                    val typeHierarchy = moduleDFG.symbolTable.typeHierarchy
                    val allPossibleCallees = mutableListOf<DataFlowIR.FunctionSymbol>()
                    typeHierarchy.inheritorsOf(call.receiverType).forEachBit {
                        val receiverType = typeHierarchy.allTypes[it]
                        if (receiverType.isAbstract) return@forEachBit
                        // TODO: Unconservative way - when we can use it?
                        //.filter { devirtualizationAnalysisResult.instantiatingClasses.contains(it) }
                        val actualCallee = when (call) {
                            is DataFlowIR.Node.VtableCall ->
                                receiverType.vtable[call.calleeVtableIndex]

                            is DataFlowIR.Node.ItableCall ->
                                receiverType.itable[call.interfaceId]!![call.calleeItableIndex]
                        }
                        allPossibleCallees.add(actualCallee)
                    }
                    if (allPossibleCallees.size <= nonDevirtualizedCallSitesUnfoldFactor)
                        staticCall(symbol, call, node, allPossibleCallees)
                    else {
                        val callSite = CallGraphNode.CallSite.Virtual(call, node, call.callee)
                        callGraph.addEdge(symbol, callSite)

                        allPossibleCallees.forEach { handleRoot(it) }
                    }
                }
            }
        }
    }
}
