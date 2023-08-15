/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.copy
import org.jetbrains.kotlin.backend.common.forEachBit
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.isBoxOrUnboxCall
import org.jetbrains.kotlin.backend.konan.util.IntArrayList
import org.jetbrains.kotlin.backend.konan.util.LongArrayList
import org.jetbrains.kotlin.backend.konan.lower.getObjectClassInstanceFunction
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.explicitParameters
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import java.util.*
import kotlin.collections.ArrayList

// Devirtualization analysis is performed using Variable Type Analysis algorithm.
// See http://web.cs.ucla.edu/~palsberg/tba/papers/sundaresan-et-al-oopsla00.pdf for details.
internal object DevirtualizationAnalysis {

    private val TAKE_NAMES = false // Take fqNames for all functions and types (for debug purposes).

    private inline fun takeName(block: () -> String) = if (TAKE_NAMES) block() else null

    fun computeRootSet(context: Context, irModule: IrModuleFragment, moduleDFG: ModuleDFG, externalModulesDFG: ExternalModulesDFG)
            : List<DataFlowIR.FunctionSymbol> {

        fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
            if (this is DataFlowIR.FunctionSymbol.External)
                return externalModulesDFG.publicFunctions[this.hash] ?: this
            return this
        }

        val entryPoint = context.ir.symbols.entryPoint?.owner
        val exported = if (entryPoint != null)
            listOf(moduleDFG.symbolTable.mapFunction(entryPoint).resolved())
        else {
            // In a library every public function and every function accessible via virtual call belongs to the rootset.
            moduleDFG.symbolTable.functionMap.values.filter {
                it is DataFlowIR.FunctionSymbol.Public
                        || (it as? DataFlowIR.FunctionSymbol.External)?.isExported == true
            } +
                    moduleDFG.symbolTable.classMap.values
                            .filterIsInstance<DataFlowIR.Type.Declared>()
                            .flatMap { it.vtable + it.itable.values.flatten() }
                            .filterIsInstance<DataFlowIR.FunctionSymbol.Declared>()
                            .filter { moduleDFG.functions.containsKey(it) }
        }

        // TODO: Are globals initializers always called whether they are actually reachable from roots or not?
        // TODO: With the changed semantics of global initializers this is no longer the case - rework.
        val globalInitializers =
                moduleDFG.symbolTable.functionMap.values.filter { it.isStaticFieldInitializer } +
                        externalModulesDFG.functionDFGs.keys.filter { it.isStaticFieldInitializer  }

        val explicitlyExported =
                moduleDFG.symbolTable.functionMap.values.filter { it.explicitlyExported } +
                        externalModulesDFG.functionDFGs.keys.filter { it.explicitlyExported }

        // Conservatively assume each associated object could be called.
        // Note: for constructors there is additional parameter (<this>) and its type will be added
        // to instantiating classes since all objects are final types.
        val associatedObjectConstructors = mutableListOf<DataFlowIR.FunctionSymbol>()
        // At this point all function references are lowered except those leaking to the native world.
        // Conservatively assume them belonging of the root set.
        val leakingThroughFunctionReferences = mutableListOf<DataFlowIR.FunctionSymbol>()
        irModule.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)

                context.getLayoutBuilder(declaration).associatedObjects.values.forEach {
                    assert(it.kind == ClassKind.OBJECT) { "An object expected but was ${it.dump()}" }
                    associatedObjectConstructors += moduleDFG.symbolTable.mapFunction(context.getObjectClassInstanceFunction(it))
                }
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                expression.acceptChildrenVoid(this)

                leakingThroughFunctionReferences.add(moduleDFG.symbolTable.mapFunction(expression.symbol.owner))
            }
        })

        return (exported + globalInitializers + explicitlyExported + associatedObjectConstructors + leakingThroughFunctionReferences).distinct()
    }

    fun BitSet.format(allTypes: Array<DataFlowIR.Type.Declared>): String {
        return allTypes.withIndex().filter { this[it.index] }.joinToString { it.value.toString() }
    }

    private val VIRTUAL_TYPE_ID = 0 // Id of [DataFlowIR.Type.Virtual].

    internal class DevirtualizationAnalysisImpl(val context: Context,
                                                val irModule: IrModuleFragment,
                                                val moduleDFG: ModuleDFG,
                                                val externalModulesDFG: ExternalModulesDFG) {

        private val entryPoint = context.ir.symbols.entryPoint?.owner

        private val symbolTable = moduleDFG.symbolTable

        sealed class Node(val id: Int) {
            var directCastEdges: MutableList<CastEdge>? = null
            var reversedCastEdges: MutableList<CastEdge>? = null

            val types = BitSet()

            var priority = -1

            var multiNodeStart = -1
            var multiNodeEnd = -1

            val multiNodeSize get() = multiNodeEnd - multiNodeStart

            fun addCastEdge(edge: CastEdge) {
                if (directCastEdges == null) directCastEdges = ArrayList(1)
                directCastEdges!!.add(edge)
                if (edge.node.reversedCastEdges == null) edge.node.reversedCastEdges = ArrayList(1)
                edge.node.reversedCastEdges!!.add(CastEdge(this, edge.suitableTypes))
            }

            abstract fun toString(allTypes: Array<DataFlowIR.Type.Declared>): String

            class Source(id: Int, typeId: Int, nameBuilder: () -> String): Node(id) {
                val name = takeName(nameBuilder)

                init {
                    types.set(typeId)
                }

                override fun toString(allTypes: Array<DataFlowIR.Type.Declared>): String {
                    return "Source(name='$name', types='${types.format(allTypes)}')"
                }
            }

            class Ordinary(id: Int, nameBuilder: () -> String) : Node(id) {
                val name = takeName(nameBuilder)

                override fun toString(allTypes: Array<DataFlowIR.Type.Declared>): String {
                    return "Ordinary(name='$name', types='${types.format(allTypes)}')"
                }
            }

            class CastEdge(val node: Node, val suitableTypes: BitSet)
        }

        class Function(val symbol: DataFlowIR.FunctionSymbol, val parameters: Array<Node>, val returns: Node, val throws: Node)

        class ExternalVirtualCall(val receiverNode: Node, val returnsNode: Node, val returnType: DataFlowIR.Type.Declared)

        inner class ConstraintGraph {

            private var nodesCount = 0

            val nodes = mutableListOf<Node>()

            val voidNode = addNode { Node.Ordinary(it, { "Void" }) }
            val virtualNode = addNode { Node.Source(it, VIRTUAL_TYPE_ID, { "Virtual" }) }
            val arrayItemField = DataFlowIR.Field(symbolTable.mapClassReferenceType(context.irBuiltIns.anyClass.owner), -1, "Array\$Item")
            val functions = mutableMapOf<DataFlowIR.FunctionSymbol, Function>()
            val externalFunctions = mutableMapOf<Pair<DataFlowIR.FunctionSymbol, DataFlowIR.Type>, Node>()
            val fields = mutableMapOf<DataFlowIR.Field, Node>() // Do not distinguish receivers.
            val virtualCallSiteReceivers = mutableMapOf<DataFlowIR.Node.VirtualCall, Node>()
            val externalVirtualCalls = mutableListOf<ExternalVirtualCall>()

            private fun nextId(): Int = nodesCount++

            fun addNode(nodeBuilder: (Int) -> Node) = nodeBuilder(nextId()).also { nodes.add(it) }
        }

        private val constraintGraph = ConstraintGraph()

        private fun DataFlowIR.Type.resolved(): DataFlowIR.Type.Declared {
            if (this is DataFlowIR.Type.Declared) return this
            val hash = (this as DataFlowIR.Type.External).hash
            return externalModulesDFG.publicTypes[hash] ?: error("Unable to resolve exported type $this")
        }

        private fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
            if (this is DataFlowIR.FunctionSymbol.External)
                return externalModulesDFG.publicFunctions[this.hash] ?: this
            return this
        }

        private inline fun forEachBitInBoth(first: BitSet, second: BitSet, block: (Int) -> Unit) {
            if (first.cardinality() < second.cardinality())
                first.forEachBit {
                    if (second[it])
                        block(it)
                }
            else second.forEachBit {
                if (first[it])
                    block(it)
            }
        }

        private inline fun IntArray.forEachEdge(v: Int, block: (Int) -> Unit) {
            for (i in this[v] until this[v + 1])
                block(this[i])
        }
        private fun IntArray.getEdge(v: Int, id: Int) = this[this[v] + id]
        private fun IntArray.edgeCount(v: Int) = this[v + 1] - this[v]

        interface TypeHierarchy {
            val allTypes: Array<DataFlowIR.Type.Declared>

            fun inheritorsOf(type: DataFlowIR.Type.Declared): BitSet
        }

        object EmptyTypeHierarchy : TypeHierarchy {
            override val allTypes: Array<DataFlowIR.Type.Declared> = emptyArray()

            override fun inheritorsOf(type: DataFlowIR.Type.Declared): BitSet {
                return BitSet()
            }
        }

        inner class TypeHierarchyImpl(override val allTypes: Array<DataFlowIR.Type.Declared>) : TypeHierarchy {
            private val typesSubTypes = Array(allTypes.size) { mutableListOf<DataFlowIR.Type.Declared>() }
            private val allInheritors = Array(allTypes.size) { BitSet() }

            init {
                val visited = BitSet()

                fun processType(type: DataFlowIR.Type.Declared) {
                    if (visited[type.index]) return
                    visited.set(type.index)
                    type.superTypes
                            .map { it.resolved() }
                            .forEach { superType ->
                                val subTypes = typesSubTypes[superType.index]
                                subTypes += type
                                processType(superType)
                            }
                }

                allTypes.forEach { processType(it) }
            }

            override fun inheritorsOf(type: DataFlowIR.Type.Declared): BitSet {
                val typeId = type.index
                val inheritors = allInheritors[typeId]
                if (!inheritors.isEmpty || type == DataFlowIR.Type.Virtual) return inheritors
                inheritors.set(typeId)
                for (subType in typesSubTypes[typeId])
                    inheritors.or(inheritorsOf(subType))
                return inheritors
            }
        }

        private fun DataFlowIR.Type.Declared.calleeAt(callSite: DataFlowIR.Node.VirtualCall) = when (callSite) {
            is DataFlowIR.Node.VtableCall ->
                vtable[callSite.calleeVtableIndex]

            is DataFlowIR.Node.ItableCall ->
                itable[callSite.interfaceId]!![callSite.calleeItableIndex]

            else -> error("Unreachable")
        }

        fun logPathToType(reversedEdges: IntArray, node: Node, type: Int) {
            val nodes = constraintGraph.nodes
            val visited = BitSet()
            val prev = mutableMapOf<Node, Node>()
            var front = mutableListOf<Node>()
            front.add(node)
            visited.set(node.id)
            lateinit var source: Node.Source
            bfs@while (front.isNotEmpty()) {
                val prevFront = front
                front = mutableListOf()
                for (from in prevFront) {
                    var endBfs = false
                    reversedEdges.forEachEdge(from.id) { toId ->
                        val to = nodes[toId]
                        if (!visited[toId] && to.types[type]) {
                            visited.set(toId)
                            prev[to] = from
                            front.add(to)
                            if (to is Node.Source) {
                                source = to
                                endBfs = true
                                return@forEachEdge
                            }
                        }
                    }
                    if (endBfs) break@bfs
                    val reversedCastEdges = from.reversedCastEdges
                    if (reversedCastEdges != null)
                        for (castEdge in reversedCastEdges) {
                            val to = castEdge.node
                            if (!visited[to.id] && castEdge.suitableTypes[type] && to.types[type]) {
                                visited.set(to.id)
                                prev[to] = from
                                front.add(to)
                                if (to is Node.Source) {
                                    source = to
                                    break@bfs
                                }
                            }
                        }
                }
            }
            try {
                var cur: Node = source
                do {
                    context.log { "    #${cur.id}" }
                    cur = prev[cur]!!
                } while (cur != node)
            } catch (t: Throwable) {
                context.log { "Unable to print path" }
            }
        }

        private inner class Condensation(val multiNodes: IntArray, val topologicalOrder: IntArray) {
            inline fun forEachNode(node: Node, block: (Node) -> Unit) {
                for (i in node.multiNodeStart until node.multiNodeEnd)
                    block(constraintGraph.nodes[multiNodes[i]])
            }
        }

        private inner class CondensationBuilder(val directEdges: IntArray, val reversedEdges: IntArray) {
            val startTime = System.currentTimeMillis()
            val nodes = constraintGraph.nodes
            val nodesCount = nodes.size
            val order = IntArray(nodesCount)
            val multiNodes = IntArray(nodesCount)
            val visited = BitSet(nodesCount)

            private fun calculateTopologicalSort() {
                require(directEdges.size == reversedEdges.size)
                var index = 0
                val nodesStack = IntArray(nodesCount)
                val edgeIdsStack = IntArray(nodesCount)
                for (nodeId in 0 until nodesCount) {
                    if (visited[nodeId]) continue
                    visited.set(nodeId)
                    nodesStack[0] = nodeId
                    edgeIdsStack[0] = 0
                    var stackPtr = 0
                    while (stackPtr != -1) {
                        val v = nodesStack[stackPtr]
                        val eid = edgeIdsStack[stackPtr]++
                        if (eid == directEdges.edgeCount(v)) {
                            order[index++] = v
                            stackPtr--
                        } else {
                            val next = directEdges.getEdge(v, eid)
                            if (!visited[next]) {
                                ++stackPtr
                                nodesStack[stackPtr] = next
                                edgeIdsStack[stackPtr] = 0
                                visited.set(next)
                            }
                        }
                    }
                }

                require(index == nodesCount)
            }

            private fun calculateMultiNodes() : IntArray {
                visited.clear()
                var index = 0
                val multiNodesInOrder = mutableListOf<Int>()
                for (i in order.size - 1 downTo 0) {
                    val nodeIndex = order[i]
                    if (visited[nodeIndex]) continue
                    multiNodesInOrder.add(nodeIndex)
                    val start = index
                    var cur = start
                    multiNodes[index++] = nodeIndex
                    visited.set(nodeIndex)
                    while (cur < index) {
                        reversedEdges.forEachEdge(multiNodes[cur++]) {
                            if (!visited[it]) {
                                multiNodes[index++] = it
                                visited.set(it)
                            }
                        }
                    }
                    val end = index
                    for (multiNodeIndex in start until end) {
                        val node = nodes[multiNodes[multiNodeIndex]]
                        node.multiNodeStart = start
                        node.multiNodeEnd = end
                    }
                }
                require(index == nodesCount)
                return multiNodesInOrder.toIntArray()
            }


            fun build(): Condensation {
                calculateTopologicalSort()
                val multiNodesInOrder = calculateMultiNodes()
                return Condensation(multiNodes, multiNodesInOrder)
            }
        }

        private fun DataFlowIR.Node.VirtualCall.debugString() =
                irCallSite?.let { ir2stringWhole(it).trimEnd() } ?: this.toString()

        fun analyze(): AnalysisResult {
            val functions = moduleDFG.functions + externalModulesDFG.functionDFGs
            assert(DataFlowIR.Type.Virtual !in symbolTable.classMap.values) {
                "DataFlowIR.Type.Virtual cannot be in symbolTable.classMap"
            }
            val allDeclaredTypes = listOf(DataFlowIR.Type.Virtual) +
                    symbolTable.classMap.values.filterIsInstance<DataFlowIR.Type.Declared>() +
                    symbolTable.primitiveMap.values.filterIsInstance<DataFlowIR.Type.Declared>() +
                    externalModulesDFG.allTypes
            val allTypes = Array<DataFlowIR.Type.Declared>(allDeclaredTypes.size) { DataFlowIR.Type.Virtual }
            for (type in allDeclaredTypes)
                allTypes[type.index] = type
            val typeHierarchy = TypeHierarchyImpl(allTypes)
            val rootSet = computeRootSet(context, irModule, moduleDFG, externalModulesDFG)

            val nodesMap = mutableMapOf<DataFlowIR.Node, Node>()

            val (instantiatingClasses, directEdges, reversedEdges) = buildConstraintGraph(nodesMap, functions, typeHierarchy, rootSet)

            context.logMultiple {
                +"FULL CONSTRAINT GRAPH"
                constraintGraph.nodes.forEach {
                    +"    NODE #${it.id}"
                    directEdges.forEachEdge(it.id) { +"        EDGE: #${it}z" }
                    it.directCastEdges?.forEach {
                        +"        CAST EDGE: #${it.node.id}z casted to ${it.suitableTypes.format(allTypes)}"
                    }
                    allTypes.forEachIndexed { index, type ->
                        if (it.types[index])
                            +"        TYPE: $type"
                    }
                }
                +""
            }

            constraintGraph.nodes.forEach {
                if (it is Node.Source) {
                    assert(reversedEdges[it.id] == reversedEdges[it.id + 1]) { "A source node #${it.id} has incoming edges" }
                    assert(it.reversedCastEdges?.isEmpty() ?: true) { "A source node #${it.id} has incoming edges" }
                }
            }

            context.logMultiple {
                val edgesCount = constraintGraph.nodes.sumOf {
                    (directEdges[it.id + 1] - directEdges[it.id]) + (it.directCastEdges?.size ?: 0)
                }
                +"CONSTRAINT GRAPH: ${constraintGraph.nodes.size} nodes, $edgesCount edges"
                +""
            }

            val condensation = CondensationBuilder(directEdges, reversedEdges).build()
            val topologicalOrder = condensation.topologicalOrder.map { constraintGraph.nodes[it] }

            context.logMultiple {
                +"CONDENSATION"
                topologicalOrder.forEachIndexed { index, multiNode ->
                    +"    MULTI-NODE #$index"
                    condensation.forEachNode(multiNode) { +"        #${it.id}: ${it.toString(allTypes)}" }
                }
                +""
            }

            topologicalOrder.forEachIndexed { index, multiNode ->
                condensation.forEachNode(multiNode) { node -> node.priority = index }
            }

            val badEdges = mutableListOf<Pair<Node, Node.CastEdge>>()
            for (node in constraintGraph.nodes) {
                node.directCastEdges
                        ?.filter { it.node.priority < node.priority } // Contradicts topological order.
                        ?.forEach { badEdges += node to it }
            }
            badEdges.sortBy { it.second.node.priority } // Heuristic.

            // First phase - greedy phase.
            var iterations = 0
            val maxNumberOfIterations = 2
            do {
                ++iterations
                // Handle all 'right-directed' edges.
                // TODO: this is pessimistic handling of [DataFlowIR.Type.Virtual], think how to do it better.
                for (multiNode in topologicalOrder) {
                    if (multiNode.multiNodeSize == 1 && multiNode is Node.Source)
                        continue // A source has no incoming edges.
                    val types = BitSet()
                    condensation.forEachNode(multiNode) { node ->
                        reversedEdges.forEachEdge(node.id) {
                            types.or(constraintGraph.nodes[it].types)
                        }
                        node.reversedCastEdges
                                ?.filter { it.node.priority < node.priority } // Doesn't contradict topological order.
                                ?.forEach {
                                    val sourceTypes = it.node.types.copy()
                                    sourceTypes.and(it.suitableTypes)
                                    types.or(sourceTypes)
                                }
                    }
                    condensation.forEachNode(multiNode) { node -> node.types.or(types) }
                }
                if (iterations >= maxNumberOfIterations) break

                var end = true
                for ((sourceNode, edge) in badEdges) {
                    val distNode = edge.node
                    val missingTypes = sourceNode.types.copy().apply { andNot(distNode.types) }
                    missingTypes.and(edge.suitableTypes)
                    if (!missingTypes.isEmpty) {
                        end = false
                        distNode.types.or(missingTypes)
                    }
                }
            } while (!end)

            // Second phase - do BFS.
            val nodesCount = constraintGraph.nodes.size
            val marked = BitSet(nodesCount)
            var front = IntArray(nodesCount)
            var prevFront = IntArray(nodesCount)
            var frontSize = 0
            val tempBitSet = BitSet()
            for ((sourceNode, edge) in badEdges) {
                val distNode = edge.node
                tempBitSet.clear()
                tempBitSet.or(sourceNode.types)
                tempBitSet.andNot(distNode.types)
                tempBitSet.and(edge.suitableTypes)
                distNode.types.or(tempBitSet)
                if (!marked[distNode.id] && !tempBitSet.isEmpty) {
                    marked.set(distNode.id)
                    front[frontSize++] = distNode.id
                }
            }

            while (frontSize > 0) {
                val prevFrontSize = frontSize
                frontSize = 0
                val temp = front
                front = prevFront
                prevFront = temp
                for (i in 0 until prevFrontSize) {
                    marked[prevFront[i]] = false
                    val node = constraintGraph.nodes[prevFront[i]]
                    directEdges.forEachEdge(node.id) { distNodeId ->
                        val distNode = constraintGraph.nodes[distNodeId]
                        if (marked[distNode.id])
                            distNode.types.or(node.types)
                        else {
                            tempBitSet.clear()
                            tempBitSet.or(node.types)
                            tempBitSet.andNot(distNode.types)
                            distNode.types.or(node.types)
                            if (!marked[distNode.id] && !tempBitSet.isEmpty) {
                                marked.set(distNode.id)
                                front[frontSize++] = distNode.id
                            }
                        }
                    }
                    node.directCastEdges?.forEach { edge ->
                        val distNode = edge.node
                        tempBitSet.clear()
                        tempBitSet.or(node.types)
                        tempBitSet.andNot(distNode.types)
                        tempBitSet.and(edge.suitableTypes)
                        distNode.types.or(tempBitSet)
                        if (!marked[distNode.id] && !tempBitSet.isEmpty) {
                            marked.set(distNode.id)
                            front[frontSize++] = distNode.id
                        }
                    }
                }
            }

            if (entryPoint == null)
                propagateFinalTypesFromExternalVirtualCalls(directEdges)

            context.logMultiple {
                topologicalOrder.forEachIndexed { index, multiNode ->
                    +"Types of multi-node #$index"
                    condensation.forEachNode(multiNode) { node ->
                        +"    Node #${node.id}"
                        allTypes.asSequence()
                                .withIndex()
                                .filter { node.types[it.index] }.toList()
                                .forEach { +"        ${it.value}" }
                    }
                }
                +""
            }

            val result = mutableMapOf<DataFlowIR.Node.VirtualCall, Pair<DevirtualizedCallSite, DataFlowIR.FunctionSymbol>>()
            val nothing = symbolTable.classMap[context.ir.symbols.nothing.owner]
            for (function in functions.values) {
                if (!constraintGraph.functions.containsKey(function.symbol)) continue
                function.body.forEachNonScopeNode { node ->
                    val virtualCall = node as? DataFlowIR.Node.VirtualCall ?: return@forEachNonScopeNode
                    assert(nodesMap[virtualCall] != null) { "Node for virtual call $virtualCall has not been built" }
                    val receiverNode = constraintGraph.virtualCallSiteReceivers[virtualCall]
                            ?: error("virtualCallSiteReceivers were not built for virtual call $virtualCall")
                    if (receiverNode.types[VIRTUAL_TYPE_ID]) {
                        context.logMultiple {
                            +"Unable to devirtualize callsite ${virtualCall.debugString()}"
                            +"from ${function.symbol}"
                            +"    receiver is Virtual"
                            logPathToType(reversedEdges, receiverNode, VIRTUAL_TYPE_ID)
                            +""
                        }
                        return@forEachNonScopeNode
                    }

                    context.logMultiple {
                        +"Devirtualized callsite ${virtualCall.debugString()}"
                        +"from ${function.symbol}"
                    }
                    val receiverType = virtualCall.receiverType.resolved()
                    val possibleReceivers = mutableListOf<DataFlowIR.Type.Declared>()
                    forEachBitInBoth(receiverNode.types, typeHierarchy.inheritorsOf(receiverType)) {
                        val type = allTypes[it]
                        assert(instantiatingClasses[it]) { "Non-instantiating class $type" }
                        if (type != nothing) {
                            context.logMultiple {
                                +"Path to type $type"
                                logPathToType(reversedEdges, receiverNode, it)
                            }
                            possibleReceivers.add(type)
                        }
                    }
                    context.log { "" }

                    result[virtualCall] = DevirtualizedCallSite(virtualCall.callee.resolved(),
                            possibleReceivers.map { possibleReceiverType ->
                                val callee = possibleReceiverType.calleeAt(virtualCall)
                                if (callee is DataFlowIR.FunctionSymbol.Declared && callee.symbolTableIndex < 0)
                                    error("Function ${possibleReceiverType}.$callee cannot be called virtually," +
                                            " but actually is at call site: ${virtualCall.debugString()}")
                                DevirtualizedCallee(possibleReceiverType, callee)
                            }) to function.symbol

                }
            }

            context.logMultiple {
                +"Devirtualized from current module:"
                result.forEach { (virtualCall, devirtualizedCallSite) ->
                    if (virtualCall.irCallSite != null) {
                        +"DEVIRTUALIZED"
                        +"FUNCTION: ${devirtualizedCallSite.second}"
                        +"CALL SITE: ${virtualCall.debugString()}"
                        +"POSSIBLE RECEIVERS:"
                        devirtualizedCallSite.first.possibleCallees.forEach { +"    TYPE: ${it.receiverType}" }
                        devirtualizedCallSite.first.possibleCallees.forEach { +"    FUN: ${it.callee}" }
                        +""
                    }
                }
                +"Devirtualized from external modules:"
                result.forEach { (virtualCall, devirtualizedCallSite) ->
                    if (virtualCall.irCallSite == null) {
                        +"DEVIRTUALIZED"
                        +"FUNCTION: ${devirtualizedCallSite.second}"
                        +"CALL SITE: ${virtualCall.debugString()}"
                        +"POSSIBLE RECEIVERS:"
                        devirtualizedCallSite.first.possibleCallees.forEach { +"    TYPE: ${it.receiverType}" }
                        devirtualizedCallSite.first.possibleCallees.forEach { +"    FUN: ${it.callee}" }
                        +""
                    }
                }
            }

            return AnalysisResult(result.asSequence().associateBy({ it.key }, { it.value.first }), typeHierarchy)
        }

        /*
         * If a virtual function is called on a receiver coming from external world and
         * the return type of the function is a final class, then we conservatively assume
         * that instance of this class could have been created by the call.
         */
        private fun propagateFinalTypesFromExternalVirtualCalls(directEdges: IntArray) {
            val nodesCount = constraintGraph.nodes.size
            constraintGraph.externalVirtualCalls
                    .groupBy { it.returnType }
                    .forEach { (type, list) ->
                        val visited = BitSet(nodesCount)
                        val stack = mutableListOf<Node>()
                        list.forEach { call ->
                            val returnsNode = call.returnsNode
                            if (call.receiverNode.types[VIRTUAL_TYPE_ID] // Called from external world.
                                    && !returnsNode.types[type.index] && !visited[returnsNode.id]
                            ) {
                                returnsNode.types.set(type.index)
                                stack.push(returnsNode)
                                visited.set(returnsNode.id)
                            }
                        }
                        while (stack.isNotEmpty()) {
                            val node = stack.pop()
                            directEdges.forEachEdge(node.id) { distNodeId ->
                                val distNode = constraintGraph.nodes[distNodeId]
                                if (!distNode.types[type.index] && !visited[distNode.id]) {
                                    distNode.types.set(type.index)
                                    visited.set(distNode.id)
                                    stack.push(distNode)
                                }
                            }
                            node.directCastEdges?.forEach { edge ->
                                val distNode = edge.node
                                if (!distNode.types[type.index] && !visited[distNode.id] && edge.suitableTypes[type.index]) {
                                    distNode.types.set(type.index)
                                    visited.set(distNode.id)
                                    stack.push(distNode)
                                }
                            }
                        }
                    }
        }

        // Both [directEdges] and [reversedEdges] are the array representation of a graph:
        // for each node v the edges of that node are stored in edges[edges[v] until edges[v + 1]].
        private data class ConstraintGraphBuildResult(val instantiatingClasses: BitSet,
                                                      val directEdges: IntArray, val reversedEdges: IntArray)

        // Here we're dividing the build process onto two phases:
        // 1. build bag of edges and direct edges array;
        // 2. build reversed edges array from the direct edges array.
        // This is to lower memory usage (all of these edges structures are more or less equal by size),
        // and by that we're only holding references to two out of three of them.
        private fun buildConstraintGraph(nodesMap: MutableMap<DataFlowIR.Node, Node>,
                                         functions: Map<DataFlowIR.FunctionSymbol, DataFlowIR.Function>,
                                         typeHierarchy: TypeHierarchyImpl,
                                         rootSet: List<DataFlowIR.FunctionSymbol>
        ): ConstraintGraphBuildResult {
            val precursor = buildConstraintGraphPrecursor(nodesMap, functions, typeHierarchy, rootSet)
            return ConstraintGraphBuildResult(precursor.instantiatingClasses, precursor.directEdges,
                    buildReversedEdges(precursor.directEdges, precursor.reversedEdgesCount))
        }

        private class ConstraintGraphPrecursor(val instantiatingClasses: BitSet,
                                               val directEdges: IntArray, val reversedEdgesCount: IntArrayList)

        private fun buildReversedEdges(directEdges: IntArray, reversedEdgesCount: IntArrayList): IntArray {
            val numberOfNodes = constraintGraph.nodes.size
            var edgesArraySize = numberOfNodes + 1
            for (v in 0 until numberOfNodes)
                edgesArraySize += reversedEdgesCount[v]
            val reversedEdges = IntArray(edgesArraySize)
            var index = numberOfNodes + 1
            for (v in 0..numberOfNodes) {
                reversedEdges[v] = index
                index += reversedEdgesCount[v]
                reversedEdgesCount[v] = 0
            }
            for (from in 0 until numberOfNodes) {
                directEdges.forEachEdge(from) { to ->
                    reversedEdges[reversedEdges[to] + (reversedEdgesCount[to]++)] = from
                }
            }
            return reversedEdges
        }

        private fun buildConstraintGraphPrecursor(nodesMap: MutableMap<DataFlowIR.Node, Node>,
                                                  functions: Map<DataFlowIR.FunctionSymbol, DataFlowIR.Function>,
                                                  typeHierarchy: TypeHierarchyImpl,
                                                  rootSet: List<DataFlowIR.FunctionSymbol>
        ): ConstraintGraphPrecursor {
            val constraintGraphBuilder = ConstraintGraphBuilder(nodesMap, functions, typeHierarchy, rootSet, true)
            constraintGraphBuilder.build()
            val bagOfEdges = constraintGraphBuilder.bagOfEdges
            val directEdgesCount = constraintGraphBuilder.directEdgesCount
            val reversedEdgesCount = constraintGraphBuilder.reversedEdgesCount
            val numberOfNodes = constraintGraph.nodes.size
            // numberOfNodes + 1 for convenience.
            directEdgesCount.reserve(numberOfNodes + 1)
            reversedEdgesCount.reserve(numberOfNodes + 1)
            var edgesArraySize = numberOfNodes + 1
            for (v in 0 until numberOfNodes)
                edgesArraySize += directEdgesCount[v]
            val directEdges = IntArray(edgesArraySize)
            var index = numberOfNodes + 1
            for (v in 0..numberOfNodes) {
                directEdges[v] = index
                index += directEdgesCount[v]
                directEdgesCount[v] = 0
            }
            for (bucket in bagOfEdges)
                if (bucket != null)
                    for (edge in bucket) {
                        val from = edge.toInt()
                        val to = (edge shr 32).toInt()
                        directEdges[directEdges[from] + (directEdgesCount[from]++)] = to
                    }
            return ConstraintGraphPrecursor(constraintGraphBuilder.instantiatingClasses, directEdges, reversedEdgesCount)
        }

        private class ConstraintGraphVirtualCall(val caller: Function, val virtualCall: DataFlowIR.Node.VirtualCall,
                                                 val arguments: List<Node>, val returnsNode: Node)

        private inner class ConstraintGraphBuilder(val functionNodesMap: MutableMap<DataFlowIR.Node, Node>,
                                                   val functions: Map<DataFlowIR.FunctionSymbol, DataFlowIR.Function>,
                                                   val typeHierarchy: TypeHierarchyImpl,
                                                   val rootSet: List<DataFlowIR.FunctionSymbol>,
                                                   val useTypes: Boolean) {

            private val allTypes = typeHierarchy.allTypes
            private val variables = mutableMapOf<DataFlowIR.Node.Variable, Node>()
            private val typesVirtualCallSites = Array(allTypes.size) { mutableListOf<ConstraintGraphVirtualCall>() }
            private val suitableTypes = arrayOfNulls<BitSet?>(allTypes.size)
            private val concreteClasses = arrayOfNulls<Node?>(allTypes.size)
            private val virtualTypeFilter = BitSet().apply { set(VIRTUAL_TYPE_ID) }
            val instantiatingClasses = BitSet()

            private val preliminaryNumberOfNodes =
                    allTypes.size + // A possible source node for each type.
                            functions.size * 2 + // <returns> and <throws> nodes for each function.
                            functions.values.sumOf {
                                it.body.allScopes.sumOf { it.nodes.size } // A node for each DataFlowIR.Node.
                            } +
                            functions.values
                                    .sumOf { function ->
                                        function.body.allScopes.sumOf {
                                            it.nodes.count { node ->
                                                // A cast if types are different.
                                                node is DataFlowIR.Node.Call
                                                        && node.returnType.resolved() != node.callee.returnParameter.type.resolved()
                                            }
                                        }
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

            // A heuristic: the number of edges in the data flow graph
            // for any reasonable program is linear in number of nodes.
            val bagOfEdges = arrayOfNulls<LongArrayList>(makePrime(preliminaryNumberOfNodes * 5))
            val directEdgesCount = IntArrayList()
            val reversedEdgesCount = IntArrayList()

            @OptIn(ExperimentalUnsignedTypes::class)
            private fun addEdge(from: Node, to: Node) {
                val fromId = from.id
                val toId = to.id
                val value = fromId.toLong() or (toId.toLong() shl 32)
                // This is 64-bit extension of a hashing method from Knuth's "The Art of Computer Programming".
                // The magic constant is the closest prime to 2^64 * phi, where phi is the golden ratio.
                val bucketIdx = ((value.toULong() * 11400714819323198393UL) % bagOfEdges.size.toULong()).toInt()
                val bucket = bagOfEdges[bucketIdx] ?: LongArrayList().also { bagOfEdges[bucketIdx] = it }
                for (x in bucket)
                    if (x == value) return
                bucket.add(value)

                directEdgesCount.reserve(fromId + 1)
                directEdgesCount[fromId]++
                reversedEdgesCount.reserve(toId + 1)
                reversedEdgesCount[toId]++
            }

            private fun concreteType(type: DataFlowIR.Type.Declared): Int {
                assert(!(type.isAbstract && type.isFinal)) { "Incorrect type: $type" }
                return if (type.isAbstract)
                    VIRTUAL_TYPE_ID
                else {
                    if (!instantiatingClasses[type.index])
                        error("Type $type is not instantiated")
                    type.index
                }
            }

            private fun ordinaryNode(nameBuilder: () -> String) =
                    constraintGraph.addNode { Node.Ordinary(it, nameBuilder) }

            private fun sourceNode(typeId: Int, nameBuilder: () -> String) =
                    constraintGraph.addNode { Node.Source(it, typeId, nameBuilder) }

            private fun concreteClass(type: DataFlowIR.Type.Declared) =
                    concreteClasses[type.index]
                            ?: sourceNode(concreteType(type)) { "Class\$$type" }.also { concreteClasses[type.index] = it}

            private fun fieldNode(field: DataFlowIR.Field) =
                    constraintGraph.fields.getOrPut(field) {
                        val fieldNode = ordinaryNode { "Field\$$field" }
                        if (entryPoint == null) {
                            // TODO: This is conservative.
                            val fieldType = field.type.resolved()
                            // Some user of our library might place some value into the field.
                            if (fieldType.isFinal)
                                addEdge(concreteClass(fieldType), fieldNode)
                            else
                                addEdge(constraintGraph.virtualNode, fieldNode)
                        }
                        fieldNode
                    }

            private var stack = mutableListOf<DataFlowIR.FunctionSymbol>()

            fun build() {
                // Rapid Type Analysis: find all instantiations and conservatively estimate call graph.

                // Add all final parameters of the roots.
                for (root in rootSet) {
                    root.parameters
                            .map { it.type.resolved() }
                            .filter { it.isFinal }
                            .forEach { addInstantiatingClass(it) }
                }
                if (entryPoint == null) {
                    // For library assume all public non-abstract classes could be instantiated.
                    // Note: for constructors there is additional parameter (<this>) and for associated objects
                    // its type will be added to instantiating classes since all objects are final types.
                    symbolTable.classMap.values
                            .filterIsInstance<DataFlowIR.Type.Public>()
                            .filter { !it.isAbstract }
                            .forEach { addInstantiatingClass(it) }
                } else {
                    // String arguments are implicitly put into the <args> array parameter of <main>.
                    addInstantiatingClass(symbolTable.mapType(context.irBuiltIns.stringType).resolved())
                    addEdge(concreteClass(symbolTable.mapType(context.irBuiltIns.stringType).resolved()),
                            fieldNode(constraintGraph.arrayItemField))
                }
                rootSet.forEach { createFunctionConstraintGraph(it, true) }
                while (stack.isNotEmpty()) {
                    val symbol = stack.pop()
                    val function = functions[symbol] ?: continue
                    val body = function.body
                    val functionConstraintGraph = constraintGraph.functions[symbol]!!

                    body.forEachNonScopeNode {
                        val node = dfgNodeToConstraintNode(functionConstraintGraph, it)
                        if (it is DataFlowIR.Node.Variable) {
                            generateVariableEdges(functionConstraintGraph, it, node)
                        }
                    }
                    addEdge(functionNodesMap[body.returns]!!, functionConstraintGraph.returns)
                    addEdge(functionNodesMap[body.throws]!!, functionConstraintGraph.throws)

                    context.logMultiple {
                        +"CONSTRAINT GRAPH FOR $symbol"
                        val ids = function.body.allScopes.flatMap { it.nodes }.withIndex().associateBy({ it.value }, { it.index })
                        function.body.forEachNonScopeNode { node ->
                            +"FT NODE #${ids[node]}"
                            +DataFlowIR.Function.nodeToString(node, ids)
                            val constraintNode = functionNodesMap[node] ?: variables[node] ?: return@forEachNonScopeNode
                            +"       CG NODE #${constraintNode.id}: ${constraintNode.toString(allTypes)}"
                        }
                        +"Returns: #${ids[function.body.returns]}"
                        +""
                    }
                }

                suitableTypes.forEach {
                    it?.and(instantiatingClasses)
                    it?.set(VIRTUAL_TYPE_ID)
                }
            }

            private fun createFunctionConstraintGraph(symbol: DataFlowIR.FunctionSymbol, isRoot: Boolean): Function? {
                if (symbol is DataFlowIR.FunctionSymbol.External) return null
                constraintGraph.functions[symbol]?.let { return it }

                val parameters = Array(symbol.parameters.size) { ordinaryNode { "Param#$it\$$symbol" } }
                if (isRoot) {
                    // Exported function from the current module.
                    symbol.parameters.forEachIndexed { index, type ->
                        val resolvedType = type.type.resolved()
                        val node = if (!resolvedType.isFinal)
                            constraintGraph.virtualNode // TODO: OBJC-INTEROP-GENERATED-CLASSES
                        else
                            concreteClass(resolvedType)
                        addEdge(node, parameters[index])
                    }
                }

                val returnsNode = ordinaryNode { "Returns\$$symbol" }
                val throwsNode = ordinaryNode { "Throws\$$symbol" }
                val functionConstraintGraph = Function(symbol, parameters, returnsNode, throwsNode)
                constraintGraph.functions[symbol] = functionConstraintGraph

                stack.push(symbol)

                return functionConstraintGraph
            }

            private fun addInstantiatingClass(type: DataFlowIR.Type.Declared) {
                if (instantiatingClasses[type.index]) return
                instantiatingClasses.set(type.index)
                context.log { "Adding instantiating class: $type" }
                checkSupertypes(type, type, BitSet())
            }

            private fun processVirtualCall(virtualCall: ConstraintGraphVirtualCall,
                                           receiverType: DataFlowIR.Type.Declared) {
                context.logMultiple {
                    +"Processing virtual call: ${virtualCall.virtualCall.callee}"
                    +"Receiver type: $receiverType"
                }
                val callee = receiverType.calleeAt(virtualCall.virtualCall)
                addEdge(doCall(virtualCall.caller, callee, virtualCall.arguments,
                        callee.returnParameter.type.resolved()), virtualCall.returnsNode)
            }

            private fun checkSupertypes(type: DataFlowIR.Type.Declared,
                                        inheritor: DataFlowIR.Type.Declared,
                                        seenTypes: BitSet) {
                seenTypes.set(type.index)

                context.logMultiple {
                    +"Checking supertype $type of $inheritor"
                    typesVirtualCallSites[type.index].let {
                        if (it.isEmpty())
                            +"None virtual call sites encountered yet"
                        else {
                            +"Virtual call sites:"
                            it.forEach { +"    ${it.virtualCall.callee}" }
                        }
                    }
                    +""
                }

                typesVirtualCallSites[type.index].let { virtualCallSites ->
                    var index = 0
                    while (index < virtualCallSites.size) {
                        processVirtualCall(virtualCallSites[index], inheritor)
                        ++index
                    }
                }
                for (superType in type.superTypes) {
                    val resolvedSuperType = superType.resolved()
                    if (!seenTypes[resolvedSuperType.index])
                        checkSupertypes(resolvedSuperType, inheritor, seenTypes)
                }
            }

            private fun createCastEdge(node: Node, type: DataFlowIR.Type.Declared): Node.CastEdge {
                if (suitableTypes[type.index] == null)
                    suitableTypes[type.index] = typeHierarchy.inheritorsOf(type).copy()
                return Node.CastEdge(node, suitableTypes[type.index]!!)
            }

            private fun doCast(function: Function, node: Node, type: DataFlowIR.Type.Declared): Node {
                val castNode = ordinaryNode { "Cast\$${function.symbol}" }
                val castEdge = createCastEdge(castNode, type)
                node.addCastEdge(castEdge)
                return castNode
            }

            private fun castIfNeeded(function: Function, node: Node,
                                     nodeType: DataFlowIR.Type.Declared, type: DataFlowIR.Type.Declared) =
                    if (!useTypes || type == nodeType)
                        node
                    else doCast(function, node, type)

            private fun edgeToConstraintNode(function: Function,
                                             edge: DataFlowIR.Edge): Node {
                val result = dfgNodeToConstraintNode(function, edge.node)
                val castToType = edge.castToType?.resolved() ?: return result
                return doCast(function, result, castToType)
            }

            fun doCall(caller: Function, callee: Function, arguments: List<Node>, returnType: DataFlowIR.Type.Declared): Node {
                assert(callee.parameters.size == arguments.size) {
                    "Function ${callee.symbol} takes ${callee.parameters.size} but caller ${caller.symbol}" +
                            " provided ${arguments.size}"
                }
                callee.parameters.forEachIndexed { index, parameter ->
                    addEdge(arguments[index], parameter)
                }
                return castIfNeeded(caller, callee.returns, callee.symbol.returnParameter.type.resolved(), returnType)
            }

            fun doCall(caller: Function, callee: DataFlowIR.FunctionSymbol,
                       arguments: List<Node>, returnType: DataFlowIR.Type.Declared): Node {
                val resolvedCallee = callee.resolved()
                val calleeConstraintGraph = createFunctionConstraintGraph(resolvedCallee, false)
                return if (calleeConstraintGraph == null) {
                    constraintGraph.externalFunctions.getOrPut(resolvedCallee to returnType) {
                        val fictitiousReturnNode = ordinaryNode { "External$resolvedCallee" }
                        if (returnType.isFinal) {
                            addInstantiatingClass(returnType)
                            addEdge(concreteClass(returnType), fictitiousReturnNode)
                        } else {
                            addEdge(constraintGraph.virtualNode, fictitiousReturnNode)
                            // TODO: Unconservative way - when we can use it?
                            // TODO: OBJC-INTEROP-GENERATED-CLASSES
//                                typeHierarchy.inheritorsOf(returnType)
//                                        .filterNot { it.isAbstract }
//                                        .filter { instantiatingClasses.containsKey(it) }
//                                        .forEach { concreteClass(it).addEdge(fictitiousReturnNode) }
                        }
                        fictitiousReturnNode
                    }
                } else {
                    addEdge(calleeConstraintGraph.throws, caller.throws)
                    doCall(caller, calleeConstraintGraph, arguments, returnType)
                }
            }


            fun generateVariableEdges(function: Function, node: DataFlowIR.Node.Variable, variableNode: Node) {
                for (value in node.values) {
                    addEdge(edgeToConstraintNode(function, value), variableNode)
                }
                if (node.kind == DataFlowIR.VariableKind.CatchParameter)
                    function.throws.addCastEdge(createCastEdge(variableNode, node.type.resolved()))
            }

            /**
             * Takes a function DFG's node and creates a constraint graph node corresponding to it.
             * Also creates all necessary edges, except for variable nodes.
             * For variable nodes edges must be created separately, otherwise recursion can be too deep.
             */
            private fun dfgNodeToConstraintNode(function: Function, node: DataFlowIR.Node): Node {

                fun edgeToConstraintNode(edge: DataFlowIR.Edge): Node =
                        edgeToConstraintNode(function, edge)

                fun doCall(callee: DataFlowIR.FunctionSymbol, arguments: List<Node>,
                           returnType: DataFlowIR.Type.Declared) =
                        doCall(function, callee, arguments, returnType)

                fun readField(field: DataFlowIR.Field, actualType: DataFlowIR.Type.Declared): Node {
                    val fieldNode = fieldNode(field)
                    val expectedType = field.type.resolved()
                    return if (!useTypes || actualType == expectedType)
                        fieldNode
                    else
                        doCast(function, fieldNode, actualType)
                }

                fun writeField(field: DataFlowIR.Field, value: Node) = addEdge(value, fieldNode(field))

                if (node is DataFlowIR.Node.Variable && node.kind != DataFlowIR.VariableKind.Temporary) {
                    return variables.getOrPut(node) {
                        ordinaryNode { "Variable\$${function.symbol}" }
                    }
                }

                return functionNodesMap.getOrPut(node) {
                    when (node) {
                        is DataFlowIR.Node.Const -> {
                            val type = node.type.resolved()
                            addInstantiatingClass(type)
                            sourceNode(concreteType(type)) { "Const\$${function.symbol}" }
                        }

                        DataFlowIR.Node.Null -> constraintGraph.voidNode

                        is DataFlowIR.Node.Parameter ->
                            function.parameters[node.index]

                        is DataFlowIR.Node.StaticCall -> {
                            val arguments = node.arguments.map(::edgeToConstraintNode)
                            doCall(node.callee, arguments, node.returnType.resolved())
                        }

                        is DataFlowIR.Node.NewObject -> {
                            val returnType = node.constructedType.resolved()
                            addInstantiatingClass(returnType)
                            val instanceNode = concreteClass(returnType)
                            val arguments = listOf(instanceNode) + node.arguments.map(::edgeToConstraintNode)
                            doCall(node.callee, arguments, returnType)
                            instanceNode
                        }

                        is DataFlowIR.Node.VirtualCall -> {
                            val callee = node.callee
                            val receiverType = node.receiverType.resolved()

                            context.logMultiple {
                                +"Virtual call"
                                +"Caller: ${function.symbol}"
                                +"Callee: $callee"
                                +"Receiver type: $receiverType"

                                +"Possible callees:"
                                forEachBitInBoth(typeHierarchy.inheritorsOf(receiverType), instantiatingClasses) {
                                    +allTypes[it].calleeAt(node).toString()
                                }
                                +""
                            }

                            val returnType = node.returnType.resolved()
                            val arguments = node.arguments.map(::edgeToConstraintNode)
                            val receiverNode = arguments[0]
                            if (receiverType == DataFlowIR.Type.Virtual)
                                addEdge(constraintGraph.virtualNode, receiverNode)

                            if (entryPoint == null && returnType.isFinal) {
                                // If we are in a library and facing final return type then
                                // this type can be returned by some user of this library, so propagate it explicitly.
                                addInstantiatingClass(returnType)
                            }

                            val returnsNode = ordinaryNode { "VirtualCallReturns\$${function.symbol}" }
                            if (receiverType != DataFlowIR.Type.Virtual)
                                typesVirtualCallSites[receiverType.index].add(
                                        ConstraintGraphVirtualCall(function, node, arguments, returnsNode))
                            forEachBitInBoth(typeHierarchy.inheritorsOf(receiverType), instantiatingClasses) {
                                val actualCallee = allTypes[it].calleeAt(node)
                                addEdge(doCall(actualCallee, arguments, actualCallee.returnParameter.type.resolved()), returnsNode)
                            }
                            if (entryPoint == null) {
                                // Add cast to [Virtual] edge from receiver to returns, if return type is not final.
                                // With this we're reflecting the fact that unknown function can return anything.
                                if (!returnType.isFinal) {
                                    receiverNode.addCastEdge(Node.CastEdge(returnsNode, virtualTypeFilter))
                                } else {
                                    constraintGraph.externalVirtualCalls.add(ExternalVirtualCall(receiverNode, returnsNode, returnType))
                                }
                            }
                            // An external function can throw anything.
                            receiverNode.addCastEdge(Node.CastEdge(function.throws, virtualTypeFilter))

                            constraintGraph.virtualCallSiteReceivers[node] = receiverNode
                            castIfNeeded(function, returnsNode, node.callee.returnParameter.type.resolved(), returnType)
                        }

                        is DataFlowIR.Node.Singleton -> {
                            val type = node.type.resolved()
                            addInstantiatingClass(type)
                            val instanceNode = concreteClass(type)
                            node.constructor?.let {
                                doCall(
                                        it,
                                        @OptIn(ExperimentalStdlibApi::class) buildList {
                                            add(instanceNode)
                                            node.arguments?.forEach { add(edgeToConstraintNode(it)) }
                                        },
                                        type
                                )
                            }
                            instanceNode
                        }

                        is DataFlowIR.Node.AllocInstance -> {
                            val type = node.type.resolved()
                            addInstantiatingClass(type)
                            concreteClass(type)
                        }

                        is DataFlowIR.Node.FunctionReference -> {
                            concreteClass(node.type.resolved())
                        }

                        is DataFlowIR.Node.FieldRead -> {
                            val type = node.field.type.resolved()
                            if (entryPoint == null && type.isFinal)
                                addInstantiatingClass(type)
                            readField(node.field, node.type.resolved())
                        }

                        is DataFlowIR.Node.FieldWrite -> {
                            val type = node.field.type.resolved()
                            if (entryPoint == null && type.isFinal)
                                addInstantiatingClass(type)
                            writeField(node.field, edgeToConstraintNode(node.value))
                            constraintGraph.voidNode
                        }

                        is DataFlowIR.Node.ArrayRead ->
                            readField(constraintGraph.arrayItemField, node.type.resolved())

                        is DataFlowIR.Node.ArrayWrite -> {
                            writeField(constraintGraph.arrayItemField, edgeToConstraintNode(node.value))
                            constraintGraph.voidNode
                        }

                        is DataFlowIR.Node.Variable ->
                            node.values.map { edgeToConstraintNode(it) }.let { values ->
                                ordinaryNode { "TempVar\$${function.symbol}" }.also { node ->
                                    values.forEach { addEdge(it, node) }
                                }
                            }

                        else -> error("Unreachable")
                    }
                }
            }
        }

    }

    private fun IrBuilderWithScope.irCoerce(value: IrExpression, coercion: IrFunctionSymbol?) =
            if (coercion == null)
                value
            else irCall(coercion).apply {
                require(coercion.owner.dispatchReceiverParameter == null &&
                        coercion.owner.extensionReceiverParameter == null &&
                        coercion.owner.valueParameters.size == 1
                ) { "Coercion function must be static with one value parameter" }
                putValueArgument(0, value)
            }

    private fun IrBuilderWithScope.irCoerce(value: IrExpression, coercion: DataFlowIR.FunctionSymbol.Declared?) =
            irCoerce(value, coercion?.let { it.irFunction?.symbol!! })

    sealed class PossiblyCoercedValue(private val coercion: IrFunctionSymbol?) {
        abstract fun getValue(irBuilder: IrBuilderWithScope): IrExpression
        fun getFullValue(irBuilder: IrBuilderWithScope): IrExpression = irBuilder.run { irCoerce(getValue(this), coercion) }

        class OverVariable(val value: IrVariable, coercion: IrFunctionSymbol?) : PossiblyCoercedValue(coercion) {
            override fun getValue(irBuilder: IrBuilderWithScope) = irBuilder.run { irGet(value) }
        }

        class OverExpression(val value: IrExpression, coercion: IrFunctionSymbol?) : PossiblyCoercedValue(coercion) {
            override fun getValue(irBuilder: IrBuilderWithScope) = value
        }
    }

    class DevirtualizedCallee(val receiverType: DataFlowIR.Type, val callee: DataFlowIR.FunctionSymbol)

    class DevirtualizedCallSite(val callee: DataFlowIR.FunctionSymbol, val possibleCallees: List<DevirtualizedCallee>)

    class AnalysisResult(val devirtualizedCallSites: Map<DataFlowIR.Node.VirtualCall, DevirtualizedCallSite>,
                         val typeHierarchy: DevirtualizationAnalysisImpl.TypeHierarchy)

    fun run(context: Context, irModule: IrModuleFragment, moduleDFG: ModuleDFG, externalModulesDFG: ExternalModulesDFG) =
            DevirtualizationAnalysisImpl(context, irModule, moduleDFG, externalModulesDFG).analyze()

    fun devirtualize(irModule: IrModuleFragment, context: Context, externalModulesDFG: ExternalModulesDFG,
                     devirtualizedCallSites: Map<IrCall, DevirtualizedCallSite>) {
        val symbols = context.ir.symbols
        val nativePtrEqualityOperatorSymbol = symbols.areEqualByValue[PrimitiveBinaryType.POINTER]!!
        val isSubtype = symbols.isSubtype
        val optimize = context.shouldOptimize()

        fun DataFlowIR.Type.resolved(): DataFlowIR.Type.Declared {
            if (this is DataFlowIR.Type.Declared) return this
            val hash = (this as DataFlowIR.Type.External).hash
            return externalModulesDFG.publicTypes[hash] ?: error("Unable to resolve exported type $hash")
        }

        fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
            if (this is DataFlowIR.FunctionSymbol.External)
                return externalModulesDFG.publicFunctions[this.hash] ?: this
            return this
        }

        fun <T : IrElement> IrStatementsBuilder<T>.irTemporary(parent: IrDeclarationParent, value: IrExpression, tempName: String, type: IrType): IrVariable {
            val temporary = IrVariableImpl(
                    value.startOffset, value.endOffset, IrDeclarationOrigin.IR_TEMPORARY_VARIABLE, IrVariableSymbolImpl(),
                    Name.identifier(tempName), type, isVar = false, isConst = false, isLateinit = false
            ).apply {
                this.parent = parent
                this.initializer = value
            }

            +temporary
            return temporary
        }

        // makes temporary val, in case tempName is specified
        fun <T : IrElement> IrStatementsBuilder<T>.irSplitCoercion(parent: IrDeclarationParent, expression: IrExpression, tempName: String?, actualType: IrType) =
                if (expression.isBoxOrUnboxCall()) {
                    val coercion = expression as IrCall
                    val argument = coercion.getValueArgument(0)!!
                    val symbol = coercion.symbol
                    if (tempName != null)
                        PossiblyCoercedValue.OverVariable(irTemporary(parent, argument, tempName, symbol.owner.explicitParameters.single().type), symbol)
                    else PossiblyCoercedValue.OverExpression(argument, symbol)
                } else {
                    if (tempName != null)
                        PossiblyCoercedValue.OverVariable(irTemporary(parent, expression, tempName, actualType), null)
                    else PossiblyCoercedValue.OverExpression(expression, null)
                }

        fun getTypeConversion(actualType: DataFlowIR.FunctionParameter,
                              targetType: DataFlowIR.FunctionParameter): DataFlowIR.FunctionSymbol.Declared? {
            if (actualType.boxFunction == null && targetType.boxFunction == null) return null
            if (actualType.boxFunction != null && targetType.boxFunction != null) {
                assert (actualType.type.resolved() == targetType.type.resolved())
                { "Inconsistent types: ${actualType.type} and ${targetType.type}" }
                return null
            }
            if (actualType.boxFunction == null)
                return targetType.unboxFunction!!.resolved() as DataFlowIR.FunctionSymbol.Declared
            return actualType.boxFunction.resolved() as DataFlowIR.FunctionSymbol.Declared
        }

        fun IrCallImpl.putArgument(index: Int, value: IrExpression) {
            var receiversCount = 0
            val callee = symbol.owner
            if (callee.dispatchReceiverParameter != null)
                ++receiversCount
            if (callee.extensionReceiverParameter != null)
                ++receiversCount
            if (index >= receiversCount)
                putValueArgument(index - receiversCount, value)
            else {
                if (callee.dispatchReceiverParameter != null && index == 0)
                    dispatchReceiver = value
                else
                    extensionReceiver = value
            }
        }

        fun irDevirtualizedCall(callSite: IrCall,
                                actualType: IrType,
                                actualCallee: IrSimpleFunction,
                                arguments: List<IrExpression>): IrExpression {
            val call = IrCallImpl(
                    callSite.startOffset, callSite.endOffset,
                    actualCallee.returnType,
                    actualCallee.symbol,
                    actualCallee.typeParameters.size,
                    actualCallee.valueParameters.size,
                    callSite.origin,
                    actualCallee.parentAsClass.symbol
            )
            assert(actualCallee.explicitParametersCount == arguments.size) {
                "Incorrect number of arguments: expected [${actualCallee.explicitParametersCount}] but was [${arguments.size}]\n" +
                        actualCallee.dump()
            }
            arguments.forEachIndexed { index, argument -> call.putArgument(index, argument) }
            return call.implicitCastIfNeededTo(actualType)
        }

        fun IrBuilderWithScope.irDevirtualizedCall(callSite: IrCall, actualType: IrType,
                                                   actualCallee: DataFlowIR.FunctionSymbol.Declared,
                                                   arguments: List<PossiblyCoercedValue>): IrExpression {
            return actualCallee.bridgeTarget.let { bridgeTarget ->
                if (bridgeTarget == null)
                    irDevirtualizedCall(callSite, actualType,
                            actualCallee.irFunction as IrSimpleFunction,
                            arguments.map { it.getFullValue(this@irDevirtualizedCall) }
                    )
                else {
                    val callResult = irDevirtualizedCall(callSite, actualType,
                            bridgeTarget.irFunction as IrSimpleFunction,
                            arguments.mapIndexed { index, value ->
                                val coercion = getTypeConversion(actualCallee.parameters[index], bridgeTarget.parameters[index])
                                val fullValue = value.getFullValue(this@irDevirtualizedCall)
                                coercion?.let { irCoerce(fullValue, coercion) } ?: fullValue
                            })
                    val returnCoercion = getTypeConversion(bridgeTarget.returnParameter, actualCallee.returnParameter)
                    irCoerce(callResult, returnCoercion)
                }
            }
        }

        var callSitesCount = 0
        var devirtualizedCallSitesCount = 0
        var actuallyDevirtualizedCallSitesCount = 0
        irModule.transformChildren(object : IrElementTransformer<IrDeclarationParent?> {
            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent?) =
                    super.visitDeclaration(declaration, declaration as? IrDeclarationParent ?: data)

            override fun visitCall(expression: IrCall, data: IrDeclarationParent?): IrExpression {
                expression.transformChildren(this, data)

                if (expression.superQualifierSymbol == null && expression.symbol.owner.isOverridable)
                    ++callSitesCount
                val devirtualizedCallSite = devirtualizedCallSites[expression] ?: return expression
                val possibleCallees = devirtualizedCallSite.possibleCallees.groupBy {
                    if (it.receiverType is DataFlowIR.Type.External) return expression
                    it.callee as? DataFlowIR.FunctionSymbol.Declared ?: return expression
                }.entries.map { entry ->
                    entry.key to entry.value.map { it.receiverType as DataFlowIR.Type.Declared }.distinct()
                }

                val caller = data ?: error("At this point code is expected to have been moved to a declaration: ${expression.render()}")
                val callee = expression.symbol.owner
                val owner = callee.parentAsClass
                // TODO: Think how to evaluate different unfold factors (in terms of both execution speed and code size).
                val classMaxUnfoldFactor = 3
                val interfaceMaxUnfoldFactor = 3
                val maxUnfoldFactor = if (owner.isInterface) interfaceMaxUnfoldFactor else classMaxUnfoldFactor
                ++devirtualizedCallSitesCount
                if (possibleCallees.size > maxUnfoldFactor) {
                    // Callsite too complicated to devirtualize.
                    return expression
                }
                ++actuallyDevirtualizedCallSitesCount

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val function = expression.symbol.owner
                val type = function.returnType
                val irBuilder = context.createIrBuilder((caller as IrDeclaration).symbol, startOffset, endOffset)
                irBuilder.run {
                    val dispatchReceiver = expression.dispatchReceiver!!
                    return when {
                        possibleCallees.isEmpty() -> irBlock(expression) {
                            val throwExpr = irCall(symbols.throwInvalidReceiverTypeException.owner).apply {
                                putValueArgument(0,
                                        irCallWithSubstitutedType(symbols.kClassImplConstructor.owner, listOf(dispatchReceiver.type)).apply {
                                            putValueArgument(0,
                                                    irCall(symbols.getObjectTypeInfo.owner).apply {
                                                        putValueArgument(0, dispatchReceiver)
                                                    })
                                        })
                            }
                            // Insert proper unboxing (unreachable code):
                            +irCoerce(throwExpr, context.getTypeConversion(throwExpr.type, type))
                        }

                        optimize && possibleCallees.size == 1 -> { // Monomorphic callsite.
                            irBlock(expression) {
                                val parameters = expression.getArgumentsWithIr().map { arg ->
                                    // Temporary val is not required here for a parameter, since each one is used for only one devirtualized callsite
                                    irSplitCoercion(caller, arg.second, tempName = null, arg.first.type)
                                }
                                +irDevirtualizedCall(expression, type, possibleCallees[0].first, parameters)
                            }
                        }

                        else -> irBlock(expression) {
                            val arguments = expression.getArgumentsWithIr().mapIndexed { index, arg ->
                                irSplitCoercion(caller, arg.second, "arg$index", arg.first.type)
                            }
                            val receiver = irTemporary(arguments[0].getFullValue(this@irBlock))
                            val typeInfo by lazy {
                                irTemporary(irCall(symbols.getObjectTypeInfo).apply {
                                    putValueArgument(0, irGet(receiver))
                                })
                            }

                            val branches = mutableListOf<IrBranchImpl>()
                            possibleCallees
                                    // Try to leave the most complicated case for the last,
                                    // and, hopefully, place it in the else clause.
                                    .sortedBy { it.second.size }
                                    .mapIndexedTo(branches) { index, devirtualizedCallee ->
                                        val (actualCallee, receiverTypes) = devirtualizedCallee
                                        val condition =
                                                if (optimize && index == possibleCallees.size - 1)
                                                    irTrue() // Don't check last type in optimize mode.
                                                else {
                                                    if (receiverTypes.size == 1) {
                                                        // It is faster to just compare type infos instead of a full type check.
                                                        val receiverType = receiverTypes[0]
                                                        val expectedTypeInfo = IrClassReferenceImpl(
                                                                startOffset, endOffset,
                                                                symbols.nativePtrType,
                                                                receiverType.irClass!!.symbol,
                                                                receiverType.irClass.defaultType
                                                        )
                                                        irCall(nativePtrEqualityOperatorSymbol).apply {
                                                            putValueArgument(0, irGet(typeInfo))
                                                            putValueArgument(1, expectedTypeInfo)
                                                        }
                                                    } else {
                                                        val receiverType = actualCallee.irFunction!!.parentAsClass
                                                        irCallWithSubstitutedType(isSubtype, listOf(receiverType.defaultType)).apply {
                                                            putValueArgument(0, irGet(typeInfo))
                                                        }
                                                    }
                                                }
                                        IrBranchImpl(
                                                startOffset = startOffset,
                                                endOffset = endOffset,
                                                condition = condition,
                                                result = irDevirtualizedCall(expression, type, actualCallee, arguments)
                                        )
                                    }
                            if (!optimize) { // Add else branch throwing exception for debug purposes.
                                branches.add(IrBranchImpl(
                                        startOffset = startOffset,
                                        endOffset = endOffset,
                                        condition = irTrue(),
                                        result = irCall(symbols.throwInvalidReceiverTypeException).apply {
                                            putValueArgument(0,
                                                    irCallWithSubstitutedType(symbols.kClassImplConstructor, listOf(dispatchReceiver.type)).apply {
                                                        putValueArgument(0, irGet(typeInfo))
                                                    }
                                            )
                                        })
                                )
                            }

                            +IrWhenImpl(
                                    startOffset = startOffset,
                                    endOffset = endOffset,
                                    type = type,
                                    origin = expression.origin,
                                    branches = branches
                            )
                        }
                    }
                }
            }
        }, null)
        context.logMultiple {
            +"Devirtualized: ${devirtualizedCallSitesCount * 100.0 / callSitesCount}%"
            +"Actually devirtualized: ${actuallyDevirtualizedCallSitesCount * 100.0 / callSitesCount}%"
        }
    }
}
